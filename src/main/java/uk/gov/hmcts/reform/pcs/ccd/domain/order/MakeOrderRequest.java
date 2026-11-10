package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

/**
 * What the frontend sends when a judge saves or submits their order; only a returned order has a task,
 * and only an order made for a hearing has the hearing's facts and note.
 */
public record MakeOrderRequest(Action action, OrderChange order, String taskId, HearingChange hearing) {

    public MakeOrderRequest(Action action, OrderChange order, String taskId) {
        this(action, order, taskId, null);
    }

    public enum Action {
        SAVE_DRAFT,
        SUBMIT_FOR_REVIEW
    }

    /**
     * The order as the judge submitted it: which order they are making (none yet has no id), the
     * make order form, and the order document from the docweave editor, if they have edited it.
     */
    public record OrderChange(UUID id,
                              long version,
                              OrderType orderType,
                              Map<String, Object> formData,
                              JsonNode docweaveSnapshot) {
    }

    /**
     * The hearing the order is for, with the case's facts and the hearing's note as the judge left them,
     * and the version of the hearing's facts the judge started from (null if none were recorded).
     */
    /** The hearing the order is for, with its facts and note only if the judge changed them. */
    public record HearingChange(int id, Long version, OrderStart.CaseFacts caseFacts, String note) {
    }
}
