package uk.gov.hmcts.reform.pcs.ccd.event.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.ActorDetails;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.OrderEventIT;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ReviewReason.GENERAL_ORDER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.ISSUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.RETURN_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.HIGH_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.RETURNED_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.SUBMITTED_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.ConfirmOrderReview.CONFIRM_ORDER_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;

/**
 * Caseworkers review a submitted order through the confirm order review event as the frontend drives it,
 * naming the order in the client context, and return it to its judge or issue it.
 */
@DisplayName("Confirm order review")
class ConfirmOrderReviewIT extends OrderEventIT {

    private static final String QUERY = "Which defendant does paragraph 2 mean?";

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;

    private long caseReference;
    private Actor judge;
    private Actor caseworker;
    private ExternalEvent<OrderStart, MakeOrderRequest> asJudge;
    private ExternalEvent<OrderStart, ConfirmOrderReviewRequest> asCaseworker;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        // Legacy IDAM accounts have numeric ids; review and return must work for them too.
        judge = events.registerActor(new ActorDetails(
            "123456", "judge@example.com", "A", "Judge", List.of("caseworker-pcs")));
        asJudge = events.external(caseReference, MAKE_ORDER).as(judge);
        caseworker = events.registerActor(new ActorDetails(
            "654321", "caseworker@example.com", "A", "Caseworker", List.of("caseworker-pcs")));
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
            .containsExactly("Open the order to review from its task");
        choose(asJudge.start().order().id());
        assertThat(asCaseworker.startExpectingRejection())
            .containsExactly("The order is no longer waiting for review");
    }

    @Test
    @DisplayName("returns the order to the judge with a query, for them to change and submit for review again")
    void returnsTheOrderToTheJudge() {
        Order order = judgeSubmitsOrder("the judge's order");
        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(
            RETURN_TO_JUDGE, order.id(), order.version(), "staff-review", " ", null)).errors())
            .containsExactly("Enter a query for the judge");

        var outcome = asCaseworker.submitExpectingSuccess(returnToJudge(order));

        assertThat(outcome.changed("orders", Order.class).state()).isEqualTo(RETURNED_TO_JUDGE);
        assertThat(outcome.audit().summary()).isEqualTo("Order returned to judge");
        assertThat(events.view(caseReference, caseworker).getCaseNotes()).isNullOrEmpty();
        // The step that returned the order carries the query.
        assertThat(jdbcTemplate.queryForList("SELECT step.state, entry.sender_name, entry.body FROM order_steps step "
            + "JOIN case_message_entry entry ON entry.id = step.message_entry_id WHERE step.order_id = ?", order.id()))
            .extracting(row -> row.get("state"), row -> row.get("sender_name"), row -> row.get("body"))
            .containsExactly(tuple("RETURNED_TO_JUDGE", "A Caseworker", QUERY));
        assertThat(asCaseworker.startExpectingRejection())
            .containsExactly("The order is no longer waiting for review");
        verify(taskManagementApi).completeTask(any(), any(), eq("staff-review"), any());
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.REVIEW_ORDER_QUERY), contains(QUERY),
            eq(judge.uid()));

        // The judge opens the order from their task, seeing the query, and can save changes to it.
        var asAuthor = asJudge.withClientContext(Map.of("orderId", order.id().toString()));
        asAuthor.submitExpectingSuccess(change(SAVE_DRAFT, asAuthor.start().order(), "half way"));
        Order saved = asAuthor.start().order();
        assertThat(saved.state()).isEqualTo(RETURNED_TO_JUDGE);
        assertThat(saved.formData()).containsEntry("notes", "half way");
        assertThat(saved.queryFromCaseworker()).isEqualTo(QUERY);

        Order resubmitted = asAuthor
            .submitExpectingSuccess(change(SUBMIT_FOR_REVIEW, saved, "the defendant named", "judge-query"))
            .changed("orders", Order.class);
        assertThat(resubmitted.id()).isEqualTo(order.id());
        assertThat(resubmitted.state()).isEqualTo(SUBMITTED_FOR_REVIEW);
        assertThat(asCaseworker.start().order().formData()).containsEntry("notes", "the defendant named");
        assertThat(asAuthor.startExpectingRejection())
            .containsExactly("The order is no longer waiting for you to change it");
        verify(taskManagementApi).completeTask(any(), any(), eq("judge-query"), any());
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.CONFIRM_ORDER_REVIEW),
            contains("has changed an order that was returned to them"));

        // Issuing the order closes its message thread, which nothing is waiting on any more.
        Order review = asCaseworker.start().order();
        asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, review.id(), review.version(),
            "staff-review", null, new Issue(new IssuedOrder(review.orderType(), review.formData(), null, "the order"),
                List.of(), HIGH_COURT)));
        assertThat(jdbcTemplate.queryForList("SELECT DISTINCT message.closed_at IS NOT NULL AS closed, "
            + "message.closure_note "
            + "FROM order_steps step JOIN case_message_entry entry ON entry.id = step.message_entry_id "
            + "JOIN case_message message ON message.id = entry.message_id WHERE step.order_id = ?",
            order.id())).extracting(row -> row.get("closed"), row -> row.get("closure_note"))
            .containsExactly(tuple(true, "Order issued"));
    }

    @Test
    @DisplayName("issues the order as the caseworker changed it, with its seal, and adds the review dates to the case")
    void issuesTheOrder() {
        Order order = judgeSubmitsOrder("the judge's order");
        var changed = new IssuedOrder(OrderType.SUSPENDED_POSSESSION, Map.of("notes", "the caseworker's order"), null,
            "the caseworker's wording");
        var reviewDate = new ReviewDateEntry(LocalDate.of(2027, 1, 15), GENERAL_ORDER, "Check the rent is paid");

        var outcome = asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), "staff-review", null,
            new Issue(changed, List.of(reviewDate), HIGH_COURT)));

        Issued issued = outcome.changed("orders", Issued.class);
        assertThat(issued.state()).isEqualTo(ISSUED);
        assertThat(issued.seal()).isEqualTo("HIGH_COURT");
        assertThat(outcome.audit().summary()).isEqualTo("Order issued");
        PCSCase view = events.view(caseReference, caseworker);
        assertThat(view.getCaseReviewDatesMarkdown())
            .contains("Review date 1", "15 Jan 2027", GENERAL_ORDER.getLabel(), "Check the rent is paid");
        verify(taskManagementApi).completeTask(any(), any(), eq("staff-review"), any());
        // Issued is final: neither the caseworker nor the judge can open the order again.
        assertThat(asCaseworker.startExpectingRejection())
            .containsExactly("The order is no longer waiting for review");
        assertThat(asJudge.withClientContext(Map.of("orderId", order.id().toString())).startExpectingRejection())
            .containsExactly("The order is no longer waiting for you to change it");
    }

    @Test
    @DisplayName("does not act on a copy of the order from before it was returned and resubmitted")
    void rejectsAStaleReview() {
        Order staleCopy = judgeSubmitsOrder("the judge's order");
        asCaseworker.submitExpectingSuccess(returnToJudge(staleCopy));
        var asAuthor = asJudge.withClientContext(Map.of("orderId", staleCopy.id().toString()));
        asAuthor.submitExpectingSuccess(change(SUBMIT_FOR_REVIEW, asAuthor.start().order(), "changed"));

        assertThat(asCaseworker.submitExpectingRejection(returnToJudge(staleCopy)).errors())
            .containsExactly("The order has been updated by another user. Reload it and try again");
    }

    @Test
    @DisplayName("keeps the judge's messages and the caseworker's query in one thread, each on the step it came with")
    void keepsEachStepsMessageInTheOrdersThread() {
        asJudge.submitExpectingSuccess(startDraft("the judge's order"));
        Order draft = asJudge.start().order();
        asJudge.submitExpectingSuccess(withStaffMessage(draft, "Serve this by post."));
        choose(draft.id());
        Order order = asCaseworker.start().order();

        asCaseworker.submitExpectingSuccess(returnToJudge(order));
        var asAuthor = asJudge.withClientContext(Map.of("orderId", order.id().toString()));
        Order returned = asAuthor.start().order();
        assertThat(returned.queryFromCaseworker()).isEqualTo(QUERY);
        asAuthor.submitExpectingSuccess(withStaffMessage(returned, "Paragraph 2 means the first defendant."));

        assertThat(jdbcTemplate.queryForList("SELECT step.state, entry.body, entry.message_id FROM order_steps step "
            + "JOIN case_message_entry entry ON entry.id = step.message_entry_id WHERE step.order_id = ? "
            + "ORDER BY step.id", order.id()))
            .extracting(row -> row.get("state"), row -> row.get("body"))
            .containsExactly(
                tuple("SUBMITTED_FOR_REVIEW", "Serve this by post."),
                tuple("RETURNED_TO_JUDGE", QUERY),
                tuple("SUBMITTED_FOR_REVIEW", "Paragraph 2 means the first defendant."));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM case_message_entry entry "
            + "JOIN order_steps step ON step.message_entry_id = entry.id WHERE step.order_id = ? "
            + "GROUP BY entry.message_id", Long.class, order.id())).isEqualTo(3L);
    }

    /** The judge submits the order for review with a message for court staff. */
    private static MakeOrderRequest withStaffMessage(Order order, String message) {
        return new MakeOrderRequest(SUBMIT_FOR_REVIEW, new OrderChange(order.id(), order.version(), order.orderType(),
            Map.of("notes", "the judge's order", "staff-message", List.of("yes"), "staff-message-text", message),
            order.docweaveSnapshot(), null), null);
    }

    /** The judge submits an order for review, which the caseworker then chooses to review. */
    private Order judgeSubmitsOrder(String notes) {
        asJudge.submitExpectingSuccess(startDraft(notes));
        Order draft = asJudge.start().order();
        asJudge.submitExpectingSuccess(change(SUBMIT_FOR_REVIEW, draft, notes));
        choose(draft.id());
        return asCaseworker.start().order();
    }

    private static ConfirmOrderReviewRequest returnToJudge(Order order) {
        return new ConfirmOrderReviewRequest(RETURN_TO_JUDGE, order.id(), order.version(), "staff-review", QUERY, null);
    }

    /** The caseworker chooses an order to review, which the frontend names in the client context. */
    private void choose(UUID orderId) {
        asCaseworker = asCaseworker.withClientContext(Map.of("orderId", orderId.toString()));
    }

    /** The orders row once issued. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Issued(OrderState state, String seal) {
    }
}
