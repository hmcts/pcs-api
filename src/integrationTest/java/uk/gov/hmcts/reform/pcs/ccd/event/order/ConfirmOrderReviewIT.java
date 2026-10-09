package uk.gov.hmcts.reform.pcs.ccd.event.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.OrderEventIT;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ReviewReason.GENERAL_ORDER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.ISSUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.RETURN_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.COUNTY_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.HIGH_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.RETURNED_TO_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.SUBMITTED_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.ConfirmOrderReview.CONFIRM_ORDER_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;

/**
 * Caseworkers review an order a judge submitted through the confirm order review event as the
 * frontend drives it: they open the order from their Work Allocation task, starting the event sends
 * them that order and the case, and they either return it to the judge with a query or issue it,
 * with any changes they made to it, saying how it is to be issued. The frontend names the order in
 * the client context. The judge who wrote a returned order opens it from the task its return gave
 * them, to change it and submit it for review again. Each step completes the Work Allocation task that asked
 * for it, in task management, before the event returns.
 */
@DisplayName("Confirm order review")
class ConfirmOrderReviewIT extends OrderEventIT {

    private static final String WORDING = "<p>IT IS ORDERED THAT:</p><ol><li><p>Possession.</p></li></ol>";
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
        judge = events.registerActor("A", "Judge", "caseworker-pcs");
        asJudge = events.external(caseReference, MAKE_ORDER).as(judge);
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
            .containsExactly("Open the order to review from its task");
        choose(asJudge.start().order().id());
        assertThat(asCaseworker.startExpectingRejection())
            .containsExactly("The order is no longer waiting for review");
    }

    @Test
    @DisplayName("lists the order as waiting for review on the orders tab")
    void listsTheOrderAsWaitingForReview() {
        judgeSubmitsOrder("the judge's order");

        assertThat(events.view(caseReference, caseworker).getOrdersMarkdown())
            .contains("Outright possession", "Waiting for review");
    }

    @Test
    @DisplayName("returns the order to the judge with the caseworker's query, as a note on the case")
    void returnsTheOrderToTheJudge() {
        Order order = judgeSubmitsOrder("the judge's order");

        var outcome = asCaseworker.submitExpectingSuccess(returnToJudge(order));

        assertThat(outcome.changed("orders", Order.class).state()).isEqualTo(RETURNED_TO_JUDGE);
        assertThat(steps(order)).containsExactly("SUBMITTED_FOR_REVIEW:the judge's order:null",
            "RETURNED_TO_JUDGE:the judge's order:" + QUERY);
        assertThat(jdbcTemplate.queryForMap("""
            SELECT n.type, n.created_by, n.author_idam_user_id FROM case_note n
            JOIN pcs_case c ON c.id = n.case_id WHERE c.case_reference = ?""", caseReference))
            .containsEntry("type", "ORDER")
            .containsEntry("created_by", "A Caseworker")
            .containsEntry("author_idam_user_id", UUID.fromString(caseworker.uid()));
        assertThat(events.view(caseReference, caseworker).getCaseNotes())
            .as("an order's query is not a case note").isEmpty();
        assertThat(outcome.audit().summary()).isEqualTo("Order returned to judge");
        // The staff task was completed before the event returned; the judge's is created by the scheduler.
        verify(taskManagementApi).completeTask(any(), any(), eq("staff-review"), any());
        camundaRequestsRun();
        var judgesTask = created(TaskType.REVIEW_ORDER_QUERY).getProcessVariables();
        assertThat((String) judgesTask.get("taskDescription").getValue()).contains(QUERY);
        assertThat(judgesTask.get("assigneeId").getValue()).isEqualTo(judge.uid());
        assertThat(asCaseworker.startExpectingRejection())
            .containsExactly("The order is no longer waiting for review");
    }

    @Test
    @DisplayName("lets the judge change an order returned to them, seeing the query, and submit it for review again")
    void letsTheJudgeChangeAReturnedOrder() {
        Order order = judgeSubmitsOrder("the judge's order");
        asCaseworker.submitExpectingSuccess(returnToJudge(order));

        assertThat(events.view(caseReference, judge).getOrdersMarkdown()).contains("Returned to judge");

        var asAuthor = asJudge.withClientContext(Map.of("orderId", order.id().toString()));
        Order returned = asAuthor.start().order();
        assertThat(returned.state()).isEqualTo(RETURNED_TO_JUDGE);
        assertThat(returned.queryFromCaseworker()).isEqualTo(QUERY);
        assertThat(returned.formData()).containsEntry("notes", "the judge's order");

        // The judge can save their changes and come back to them, still seeing the query.
        asAuthor.submitExpectingSuccess(change(SAVE_DRAFT, returned, "half way"));
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
        camundaRequestsRun();
        // The first submission's request may be sent after this one: the scheduler sends them in parallel.
        assertThat(createdDescriptions(TaskType.CONFIRM_ORDER_REVIEW))
            .anySatisfy(description ->
                assertThat(description).contains("has changed an order that was returned to them"));
    }

    @Test
    @DisplayName("returns the order to the judge even when task management is down")
    void returnsTheOrderWhenTaskManagementIsDown() {
        Order order = judgeSubmitsOrder("the judge's order");
        doThrow(new IllegalStateException("task management is down"))
            .when(taskManagementApi).completeTask(any(), any(), eq("staff-review"), any());

        var outcome = asCaseworker.submitExpectingSuccess(returnToJudge(order));

        assertThat(outcome.changed("orders", Order.class).state()).isEqualTo(RETURNED_TO_JUDGE);
    }

    @Test
    @DisplayName("does not act on a review that is missing what the schema cannot check")
    void rejectsAnIncompleteReview() {
        Order order = judgeSubmitsOrder("the judge's order");

        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(
            RETURN_TO_JUDGE, order.id(), order.version(), "staff-review", " ", null)).errors())
            .containsExactly("Enter a query for the judge");
        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), "staff-review", null,
            new Issue(asSubmitted(order), List.of(), true, false, false, List.of("not-a-party"), COUNTY_COURT)))
            .errors())
            .containsExactly("The order can only be served on parties to the case");
    }

    @Test
    @DisplayName("records how the order is issued, adds the review dates to the case, and keeps the order as issued")
    void issuesTheOrder() {
        Order order = judgeSubmitsOrder("the judge's order");
        String defendant = asCaseworker.start().caseContext().defendants().getFirst().id();
        var reviewDate = new ReviewDateEntry(LocalDate.of(2027, 1, 15), GENERAL_ORDER, "Check the rent is paid");

        var outcome = asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), "staff-review", null,
            new Issue(asSubmitted(order), List.of(reviewDate), true, false, false, List.of(defendant), HIGH_COURT)));

        Issued issued = outcome.changed("orders", Issued.class);
        assertThat(issued.state()).isEqualTo(ISSUED);
        assertThat(issued.nextStepsComplete()).isTrue();
        assertThat(issued.finalOrder()).isFalse();
        assertThat(issued.serveAllParties()).isFalse();
        assertThat(issued.partiesServed()).containsExactly(defendant);
        assertThat(issued.seal()).isEqualTo("HIGH_COURT");
        assertThat(issuedWording(order)).isEqualTo(WORDING);
        assertThat(outcome.audit().summary()).isEqualTo("Order issued");
        assertThat(jdbcTemplate.queryForList("""
            SELECT r.date, r.reason, r.description FROM case_review_date r
            JOIN pcs_case c ON c.id = r.case_id WHERE c.case_reference = ?""", caseReference))
            .containsExactly(Map.of(
                "date", java.sql.Date.valueOf(reviewDate.date()),
                "reason", "GENERAL_ORDER",
                "description", "Check the rent is paid"));
        verify(taskManagementApi).completeTask(any(), any(), eq("staff-review"), any());
        // Issued is final: neither the caseworker nor the judge can open the order again.
        assertThat(asCaseworker.startExpectingRejection())
            .containsExactly("The order is no longer waiting for review");
        assertThat(asJudge.withClientContext(Map.of("orderId", order.id().toString())).startExpectingRejection())
            .containsExactly("The order is no longer waiting for you to change it");
    }

    @Test
    @DisplayName("issues the order as the caseworker changed it, keeping the judge's as they submitted it")
    void issuesTheOrderAsTheCaseworkerChangedIt() {
        Order order = judgeSubmitsOrder("the judge's order");
        var changed = new IssuedOrder("SUSPENDED_POSSESSION", Map.of("notes", "the caseworker's order"), null,
            WORDING);

        asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, order.id(),
            order.version(), "staff-review", null,
            new Issue(changed, List.of(), true, true, true, List.of(), COUNTY_COURT)));

        assertThat(steps(order)).containsExactly(
            "SUBMITTED_FOR_REVIEW:the judge's order:null", "ISSUED:the caseworker's order:null");
        assertThat(events.view(caseReference, caseworker).getOrdersMarkdown())
            .contains("Suspended possession", "Issued");
    }

    @Test
    @DisplayName("does not act on a copy of the order from before it was returned and resubmitted")
    void rejectsAStaleReview() {
        Order order = judgeSubmitsOrder("the judge's order");
        Order staleCopy = asCaseworker.start().order();
        asCaseworker.submitExpectingSuccess(returnToJudge(order));
        var asAuthor = asJudge.withClientContext(Map.of("orderId", order.id().toString()));
        asAuthor.submitExpectingSuccess(change(SUBMIT_FOR_REVIEW, asAuthor.start().order(), "changed"));

        assertThat(asCaseworker.submitExpectingRejection(returnToJudge(staleCopy)).errors())
            .containsExactly("The order has been updated by another user. Reload it and try again");
        assertThat(asCaseworker.submitExpectingRejection(new ConfirmOrderReviewRequest(ISSUE, staleCopy.id(),
            staleCopy.version(), "staff-review", null,
            new Issue(asSubmitted(staleCopy), List.of(), true, true, true, List.of(), COUNTY_COURT))).errors())
            .containsExactly("The order has been updated by another user. Reload it and try again");
    }

    /** The judge submits an order for review, which the caseworker then chooses to review. */
    private Order judgeSubmitsOrder(String notes) {
        asJudge.submitExpectingSuccess(startDraft(notes));
        Order draft = asJudge.start().order();
        asJudge.submitExpectingSuccess(change(SUBMIT_FOR_REVIEW, draft, notes));
        choose(draft.id());
        return asCaseworker.start().order();
    }

    /** The order's steps since it was submitted: the state, note and query each left it with. */
    private List<String> steps(Order order) {
        return jdbcTemplate.queryForList("""
            SELECT s.state || ':' || (s.form_data->>'notes') || ':' || coalesce(n.note, 'null')
            FROM order_steps s LEFT JOIN case_note n ON n.id = s.note_id
            WHERE s.order_id = ? AND s.state <> 'DRAFT' ORDER BY s.id""", String.class, order.id());
    }

    /** The wording of the order as issued, on its issuing step. */
    private String issuedWording(Order order) {
        return jdbcTemplate.queryForObject(
            "SELECT order_html FROM order_steps WHERE order_id = ? AND state = 'ISSUED'", String.class, order.id());
    }

    private static ConfirmOrderReviewRequest returnToJudge(Order order) {
        return new ConfirmOrderReviewRequest(RETURN_TO_JUDGE, order.id(), order.version(), "staff-review", QUERY, null);
    }

    /** The judge's order as a caseworker who changed nothing issues it. */
    private static IssuedOrder asSubmitted(Order order) {
        return new IssuedOrder(order.orderType(), order.formData(), order.docweaveSnapshot(), WORDING);
    }

    /** The caseworker chooses an order to review, which the frontend names in the client context. */
    private void choose(UUID orderId) {
        asCaseworker = asCaseworker.withClientContext(Map.of("orderId", orderId.toString()));
    }

    /** The orders row once issued. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Issued(OrderState state,
                  Boolean nextStepsComplete,
                  Boolean finalOrder,
                  Boolean serveAllParties,
                  List<String> partiesServed,
                  String seal) {
    }
}
