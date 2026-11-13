package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the frontend sends when a judge saves or submits their order, with the task that brought them to it, and
 * the hearing's facts and note when made for a hearing.
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
     * The order as the judge submitted it: which order they are making (none yet has no id), what it decides about
     * each application, and the frontend's own state for reopening it: its make order form, stored and returned but
     * never read here, and the order document from the docweave editor, if they have edited it.
     */
    public record OrderChange(UUID id,
                              long version,
                              OrderType orderType,
                              Map<String, Object> formData,
                              JsonNode docweaveSnapshot,
                              List<ApplicationDecision> applicationDecisions) {

        public OrderChange {
            applicationDecisions = applicationDecisions == null ? List.of() : applicationDecisions;
        }

        public OrderChange(UUID id, long version, OrderType orderType, Map<String, Object> formData,
                           JsonNode docweaveSnapshot) {
            this(id, version, orderType, formData, docweaveSnapshot, List.of());
        }
    }

    /**
     * The hearing the order is for, with the case's facts and the hearing's note as the judge left them,
     * and the version of the hearing's facts the judge started from (null if none were recorded).
     */
    public record HearingChange(int id, Long version, OrderStart.CaseFacts caseFacts, String note) {
    }
}
