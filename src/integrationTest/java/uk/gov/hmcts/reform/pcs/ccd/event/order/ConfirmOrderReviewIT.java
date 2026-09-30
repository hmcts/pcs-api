package uk.gov.hmcts.reform.pcs.ccd.event.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Issue;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.ReviewDateEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Party;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.ClientContext;
import uk.gov.hmcts.reform.pcs.ccd.util.ClientContextRetriever;
import uk.gov.hmcts.reform.pcs.ccd.view.DraftOrdersView;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ReviewReason.GENERAL_ORDER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.ISSUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.RETURN_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState.ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState.RETURNED_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.COUNTY_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.HIGH_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.ConfirmOrderReview.CONFIRM_ORDER_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;

/**
 * Caseworkers review an order a judge submitted through the confirm order review event as the
 * frontend drives it: they choose the order on the case's draft orders tab, starting the event sends
 * them that order and the case, and they either return it to the judge with a query or say how it is
 * to be issued. The frontend names the chosen order in the Client-Context header, which the SDK's
 * test support cannot send, so the test stands in for the header's reader.
 */
@PcsCcdEventTest
@DisplayName("Confirm order review")
class ConfirmOrderReviewIT extends AbstractPostgresContainerIT {

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DraftOrdersView draftOrdersView;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @MockitoBean
    private ClientContextRetriever clientContextRetriever;

    private long caseReference;
    private Actor caseworker;
    private ExternalEvent<OrderStart, MakeOrderRequest> asJudge;
    private ExternalEvent<OrderStart, ConfirmOrderReviewRequest> asCaseworker;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        asJudge = events.external(caseReference, MAKE_ORDER)
            .as(events.registerActor("A", "Judge", "caseworker-pcs"));
        caseworker = events.registerActor("A", "Caseworker", "caseworker-pcs");
        asCaseworker = events.external(caseReference, CONFIRM_ORDER_REVIEW).as(caseworker);
    }

    @Test
    @DisplayName("shows the caseworker the order the judge submitted and the case")
    void showsTheSubmittedOrderAndTheCase() {
        Order submitted = judgeSubmitsOrder("the judge's order");

        OrderStart start = asCaseworker.start();

        assertThat(start.order().id()).isEqualTo(submitted.id());
        assertThat(start.order().formData()).containsEntry("notes", "the judge's order");
        assertThat(start.caseContext().caseReference()).isEqualTo(caseReference);
        assertThat(start.caseContext().defendants()).extracting(Party::name).containsExactly(IssuedCases.DEFENDANT);
    }

    @Test
    @DisplayName("starts only the review of a chosen order that is waiting for one")
    void refusesToStartWithoutAnOrderAwaitingReview() {
        asJudge.submitExpectingSuccess(startDraft("still a draft"));

        assertThat(asCaseworker.startExpectingRejection())
            .containsExactly("Choose an order to review from the case's Draft orders tab");
        choose(asJudge.start().order().id());
        assertThat(asCaseworker.startExpectingRejection())
            .containsExactly("The order is no longer waiting for review");
    }

    @Test
    @DisplayName("links to the review of each order waiting for one on the draft orders tab")
    void linksToTheReviewOfEachOrderWaitingForOne() {
        Order order = judgeSubmitsOrder("the judge's order");
        PCSCase pcsCase = PCSCase.builder().build();

        draftOrdersView.setCaseFields(pcsCase, pcsCaseRepository.findByCaseReference(caseReference).orElseThrow());

        assertThat(pcsCase.getDraftOrdersMarkdown())
            .contains("Outright possession", "Waiting for review")
            .contains("/case/${[CASE_REFERENCE]}/confirm-order-review?orderId=" + order.id());
    }

    @Test
    @DisplayName("returns the order to the judge with the caseworker's query")
    void returnsTheOrderToTheJudge() {
        Order order = judgeSubmitsOrder("the judge's order");

        var outcome = asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(
            RETURN_TO_JUDGE, order.id(), order.version(), "Which defendant does paragraph 2 mean?", null));

        assertThat(outcome.changed("draft_orders", Order.class).state()).isEqualTo(RETURNED_TO_JUDGE);
        Review review = outcome.changed("order_reviews", Review.class);
        assertThat(review.outcome()).isEqualTo("RETURNED_TO_JUDGE");
        assertThat(review.queryToJudge()).isEqualTo("Which defendant does paragraph 2 mean?");
        assertThat(review.reviewerIdamUserId()).isEqualTo(caseworker.uid());
        assertThat(outcome.audit().summary()).isEqualTo("Order returned to judge");
        assertThat(asCaseworker.startExpectingRejection())
            .containsExactly("The order is no longer waiting for review");
    }

    @Test
    @DisplayName("does not act on an incomplete review")
    void rejectsAnIncompleteReview() {
        Order order = judgeSubmitsOrder("the judge's order");

        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(
            RETURN_TO_JUDGE, order.id(), order.version(), " ", null)).errors())
            .containsExactly("Enter a query for the judge");
        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), null, new Issue(List.of(), true, false, false, List.of(), null))).errors())
            .containsExactly("Select who to serve the order on", "Select which seal the order should have");
        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), null, new Issue(List.of(), true, false, false, List.of("not-a-party"), COUNTY_COURT)))
            .errors())
            .containsExactly("The order can only be served on parties to the case");
    }

    @Test
    @DisplayName("records how the order is issued")
    void issuesTheOrder() {
        Order order = judgeSubmitsOrder("the judge's order");
        String defendant = asCaseworker.start().caseContext().defendants().getFirst().id();

        var outcome = asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), null, new Issue(List.of(), true, false, false, List.of(defendant), HIGH_COURT)));

        assertThat(outcome.changed("draft_orders", Order.class).state()).isEqualTo(ISSUED);
        Review review = outcome.changed("order_reviews", Review.class);
        assertThat(review.outcome()).isEqualTo("ISSUED");
        assertThat(review.nextStepsComplete()).isTrue();
        assertThat(review.finalOrder()).isFalse();
        assertThat(review.serveAllParties()).isFalse();
        assertThat(review.partiesServed()).containsExactly(defendant);
        assertThat(review.seal()).isEqualTo("HIGH_COURT");
        assertThat(outcome.audit().summary()).isEqualTo("Order issued");
    }

    @Test
    @DisplayName("adds the caseworker's review dates to the case when the order is issued")
    void addsReviewDatesToTheCase() {
        Order order = judgeSubmitsOrder("the judge's order");
        var reviewDate = new ReviewDateEntry(LocalDate.of(2027, 1, 15), GENERAL_ORDER, "Check the rent is paid");

        asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, order.id(), order.version(),
            null, new Issue(List.of(reviewDate), true, true, true, List.of(), COUNTY_COURT)));

        assertThat(jdbcTemplate.queryForList("""
            SELECT r.date, r.reason, r.description FROM case_review_date r
            JOIN pcs_case c ON c.id = r.case_id WHERE c.case_reference = ?""", caseReference))
            .containsExactly(Map.of(
                "date", java.sql.Date.valueOf(reviewDate.date()),
                "reason", "GENERAL_ORDER",
                "description", "Check the rent is paid"));
    }

    @Test
    @DisplayName("does not review an order that has changed since the caseworker opened it")
    void rejectsAStaleReview() {
        Order order = judgeSubmitsOrder("the judge's order");

        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(
            RETURN_TO_JUDGE, order.id(), order.version() - 1, "A query", null)).errors())
            .containsExactly("The order has been updated by another user. Reload it and try again");
    }

    /** The judge submits an order for review, which the caseworker then chooses to review. */
    private Order judgeSubmitsOrder(String notes) {
        asJudge.submitExpectingSuccess(startDraft(notes));
        Order draft = asJudge.start().order();
        Order submitted = asJudge.submitExpectingSuccess(new MakeOrderRequest(SUBMIT_FOR_REVIEW, new OrderChange(
                draft.id(), draft.version(), draft.orderType(), draft.formData(), draft.docweaveSnapshot())))
            .changed("draft_orders", Order.class);
        choose(submitted.id());
        return submitted;
    }

    /** The caseworker chooses an order to review, which the frontend names in the Client-Context header. */
    private void choose(UUID orderId) {
        ClientContext clientContext = new ClientContext();
        clientContext.setOrderId(orderId.toString());
        when(clientContextRetriever.getClientContext()).thenReturn(clientContext);
    }

    private static MakeOrderRequest startDraft(String notes) {
        return new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, "OUTRIGHT_POSSESSION", Map.of("notes", notes), null));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Review(String outcome,
                  String reviewerIdamUserId,
                  String queryToJudge,
                  Boolean nextStepsComplete,
                  Boolean finalOrder,
                  Boolean serveAllParties,
                  List<String> partiesServed,
                  String seal) {
    }
}
