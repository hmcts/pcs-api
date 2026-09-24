package uk.gov.hmcts.reform.pcs.ccd.event.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Order;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action.START_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action.SUBMIT_FOR_REVIEW;

/**
 * A judge making an order on one case, driving the make order event as the frontend does: open the
 * event to see the working order, then reply with an action on it. The frontend's form data is
 * opaque to pcs-api, so a single note stands in for it.
 */
final class OrderingJudge {

    private final CcdEventTestSupport<PCSCase, State> events;
    private final Actor judge;
    private final long caseReference;

    private OrderingJudge(CcdEventTestSupport<PCSCase, State> events, Actor judge, long caseReference) {
        this.events = events;
        this.judge = judge;
        this.caseReference = caseReference;
    }

    static OrderingJudge onCase(CcdEventTestSupport<PCSCase, State> events, String name, long caseReference) {
        return new OrderingJudge(events, events.registerActor(name, "Judge", "caseworker-pcs"), caseReference);
    }

    /** What the frontend shows the judge: their working order and the facts of the case. */
    MakeOrderEnvelope opens() {
        return start().payload();
    }

    Order draft() {
        return opens().order();
    }

    String draftNotes() {
        return notesIn(draft());
    }

    boolean hasADraft() {
        return draft().id() != null;
    }

    String uid() {
        return judge.uid();
    }

    void startsDraft(String notes) {
        reply(START_DRAFT, newDraft(notes)).submitExpectingSuccess();
    }

    void saves(Order draft, String notes) {
        reply(SAVE_DRAFT, withNotes(draft, notes)).submitExpectingSuccess();
    }

    CcdEventTestSupport<PCSCase, State>.Accepted submitsForReview(Order draft, String notes) {
        return reply(SUBMIT_FOR_REVIEW, withNotes(draft, notes)).submitExpectingSuccess();
    }

    /**
     * The HTTP status CCD receives when the judge tries to start a draft. pcs-api answers every
     * conflicting change with the same 409 body, so the status is the whole outcome.
     */
    int triesToStartDraft(String notes) {
        return statusOf(reply(START_DRAFT, newDraft(notes)));
    }

    /** The HTTP status CCD receives when the judge tries to save this order with new notes. */
    int triesToSave(Order order, String notes) {
        return statusOf(reply(SAVE_DRAFT, withNotes(order, notes)));
    }

    /** What pcs-api says, with HTTP 400, when the frontend sends an order action it cannot make sense of. */
    String refusalOf(Action action, Order order) {
        return reply(action, order).submitExpectingFailure(400).body();
    }

    /**
     * The one order the event changed, as it recorded it in the orders table, including the version a
     * later change must name.
     */
    static Order recorded(CcdEventTestSupport<PCSCase, State>.Accepted submission) {
        var changes = submission.changes("orders");
        if (changes.size() != 1) {
            throw new AssertionError("Expected the event to change one order, got " + changes);
        }
        JsonNode row = changes.getFirst().newValues();
        return new Order(UUID.fromString(row.path("id").asText()), OrderState.valueOf(row.path("state").asText()),
            row.path("version").asLong(), row.path("draft_payload"));
    }

    static Order newDraft(String notes) {
        return new Order(null, null, 0, notes(notes));
    }

    static String notesIn(Order order) {
        return order.draftPayload().path("notes").asText();
    }

    private static Order withNotes(Order order, String notes) {
        return new Order(order.id(), order.state(), order.version(), notes(notes));
    }

    private static JsonNode notes(String notes) {
        return JsonNodeFactory.instance.objectNode().put("notes", notes);
    }

    private CcdEventTestSupport<PCSCase, State>.Started start() {
        return events.start(caseReference, MakeOrder.EVENT_ID).as(judge).startExpectingSuccess();
    }

    private CcdEventTestSupport<PCSCase, State>.EventSubmission reply(Action action, Order order) {
        return start().submittingPayload(new MakeOrderEnvelope(action, order, null));
    }

    private static int statusOf(CcdEventTestSupport<PCSCase, State>.EventSubmission submission) {
        var result = submission.submit();
        if (result instanceof CcdEventTestSupport<PCSCase, State>.Accepted) {
            return 200;
        }
        if (result instanceof CcdEventTestSupport<PCSCase, State>.Failed failed) {
            return failed.status();
        }
        throw new AssertionError("Expected pcs-api to accept or refuse the action, got errors " + result.errors());
    }
}
