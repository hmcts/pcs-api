package uk.gov.hmcts.reform.pcs.ccd.event.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Issue;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.IssuedOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.ReviewDateEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Party;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ReviewReason.GENERAL_ORDER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.ISSUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.RETURN_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState.ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState.RETURNED_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState.SUBMITTED_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.COUNTY_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.HIGH_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.ConfirmOrderReview.CONFIRM_ORDER_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;

/**
 * Caseworkers review an order a judge submitted through the confirm order review event as the
 * frontend drives it: they choose the order on the case's draft orders tab, starting the event sends
 * them that order and the case, and they either return it to the judge with a query or issue it,
 * with any changes they made to it, saying how it is to be issued. The frontend names the chosen
 * order in the client context. The judge who wrote a returned order chooses it on the same tab, to
 * change it and submit it for review again.
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

    private long caseReference;
    private Actor judge;
    private Actor caseworker;
    private ExternalEvent<OrderStart, MakeOrderRequest> asJudge;
    private ExternalEvent<OrderStart, ConfirmOrderReviewRequest> asCaseworker;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        judge = events.registerActor("A", "Judge", "caseworker-pcs");
        asJudge = events.external(caseReference, MAKE_ORDER).as(judge);
        caseworker = events.registerActor("A", "Caseworker", "caseworker-pcs", "hearing-centre-admin");
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
    @DisplayName("links a caseworker shown the draft orders tab, and no one else, to the review of each order waiting")
    void linksToTheReviewOfEachOrderWaitingForOne() {
        Order order = judgeSubmitsOrder("the judge's order");

        assertThat(events.view(caseReference, caseworker).getDraftOrdersMarkdown())
            .contains("Outright possession", "Waiting for review")
            .contains("/cases/${[CASE_REFERENCE]}/event/ext:confirmOrderReview?expected_sub=" + caseworker.uid()
                + "&amp;orderId=" + order.id());
        assertThat(events.view(caseReference, judge).getDraftOrdersMarkdown())
            .contains("Waiting for review")
            .doesNotContain("Review order");
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
    @DisplayName("lets the judge change an order returned to them, showing the query, and submit it for review again")
    void letsTheJudgeChangeAReturnedOrder() {
        Order order = judgeSubmitsOrder("the judge's order");
        asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(
            RETURN_TO_JUDGE, order.id(), order.version(), "Which defendant does paragraph 2 mean?", null));

        assertThat(events.view(caseReference, judge).getDraftOrdersMarkdown())
            .contains("Returned to judge")
            .contains("/cases/${[CASE_REFERENCE]}/event/ext:makeOrder?expected_sub=" + judge.uid()
                + "&amp;orderId=" + order.id());
        assertThat(events.view(caseReference, caseworker).getDraftOrdersMarkdown()).doesNotContain("Change order");

        var asAuthor = asJudge.withClientContext(Map.of("orderId", order.id().toString()));
        Order returned = asAuthor.start().order();
        assertThat(returned.state()).isEqualTo(RETURNED_TO_JUDGE);
        assertThat(returned.queryFromCaseworker()).isEqualTo("Which defendant does paragraph 2 mean?");
        assertThat(returned.formData()).containsEntry("notes", "the judge's order");

        Order resubmitted = asAuthor.submitExpectingSuccess(new MakeOrderRequest(SUBMIT_FOR_REVIEW, new OrderChange(
                returned.id(), returned.version(), returned.orderType(), Map.of("notes", "the defendant named"),
                returned.docweaveSnapshot())))
            .changed("draft_orders", Order.class);
        assertThat(resubmitted.id()).isEqualTo(order.id());
        assertThat(resubmitted.state()).isEqualTo(SUBMITTED_FOR_REVIEW);
        assertThat(asCaseworker.start().order().formData()).containsEntry("notes", "the defendant named");
        assertThat(asAuthor.startExpectingRejection())
            .containsExactly("The order is no longer waiting for you to change it");
    }

    @Test
    @DisplayName("does not act on an incomplete review")
    void rejectsAnIncompleteReview() {
        Order order = judgeSubmitsOrder("the judge's order");

        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(
            RETURN_TO_JUDGE, order.id(), order.version(), " ", null)).errors())
            .containsExactly("Enter a query for the judge");
        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), null, new Issue(null, List.of(), true, true, true, List.of(), COUNTY_COURT))).errors())
            .containsExactly("Send the order as it is to be issued");
        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), null, new Issue(asSubmitted(order), List.of(), true, false, false, List.of(), null)))
            .errors())
            .containsExactly("Select who to serve the order on", "Select which seal the order should have");
        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), null,
            new Issue(asSubmitted(order), List.of(), true, false, false, List.of("not-a-party"), COUNTY_COURT)))
            .errors())
            .containsExactly("The order can only be served on parties to the case");
    }

    @Test
    @DisplayName("records how the order is issued")
    void issuesTheOrder() {
        Order order = judgeSubmitsOrder("the judge's order");
        String defendant = asCaseworker.start().caseContext().defendants().getFirst().id();

        var outcome = asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), null,
            new Issue(asSubmitted(order), List.of(), true, false, false, List.of(defendant), HIGH_COURT)));

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
    @DisplayName("issues the order as the caseworker changed it, keeping the judge's as they submitted it")
    void issuesTheOrderAsTheCaseworkerChangedIt() {
        Order order = judgeSubmitsOrder("the judge's order");
        var changed = new IssuedOrder("SUSPENDED_POSSESSION", Map.of("notes", "the caseworker's order"), null);

        var outcome = asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), null, new Issue(changed, List.of(), true, true, true, List.of(), COUNTY_COURT)));

        Order judges = outcome.changed("draft_orders", Order.class);
        assertThat(judges.orderType()).isEqualTo("OUTRIGHT_POSSESSION");
        assertThat(judges.formData()).containsEntry("notes", "the judge's order");
        Review review = outcome.changed("order_reviews", Review.class);
        assertThat(review.orderType()).isEqualTo("SUSPENDED_POSSESSION");
        assertThat(review.formData()).containsEntry("notes", "the caseworker's order");
    }

    @Test
    @DisplayName("adds the caseworker's review dates to the case when the order is issued")
    void addsReviewDatesToTheCase() {
        Order order = judgeSubmitsOrder("the judge's order");
        var reviewDate = new ReviewDateEntry(LocalDate.of(2027, 1, 15), GENERAL_ORDER, "Check the rent is paid");

        asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, order.id(), order.version(),
            null, new Issue(asSubmitted(order), List.of(reviewDate), true, true, true, List.of(), COUNTY_COURT)));

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

    /** The judge's order as a caseworker who changed nothing issues it. */
    private static IssuedOrder asSubmitted(Order order) {
        return new IssuedOrder(order.orderType(), order.formData(), order.docweaveSnapshot());
    }

    /** The caseworker chooses an order to review, which the frontend names in the client context. */
    private void choose(UUID orderId) {
        asCaseworker = asCaseworker.withClientContext(Map.of("orderId", orderId.toString()));
    }

    private static MakeOrderRequest startDraft(String notes) {
        return new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, "OUTRIGHT_POSSESSION", Map.of("notes", notes), null));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Review(String outcome,
                  String reviewerIdamUserId,
                  String queryToJudge,
                  String orderType,
                  Map<String, Object> formData,
                  Boolean nextStepsComplete,
                  Boolean finalOrder,
                  Boolean serveAllParties,
                  List<String> partiesServed,
                  String seal) {
    }
}
