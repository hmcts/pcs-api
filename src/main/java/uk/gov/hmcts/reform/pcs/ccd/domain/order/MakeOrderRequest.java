package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

/**
 * What the frontend sends when a judge saves or submits their order, with the task they opened it from:
 * the one returning it to them, or the one asking them to decide an application.
 */
public record MakeOrderRequest(Action action, OrderChange order, String taskId) {

    public enum Action {
        SAVE_DRAFT,
        SUBMIT_FOR_REVIEW
    }

    /**
     * The order as the judge submitted it: which order they are making (none yet has no id), the
     * make order form, and the order document from the docweave editor, if they have edited it. A new
     * order on an application names it.
     */
    public record OrderChange(UUID id,
                              long version,
                              OrderType orderType,
                              Map<String, Object> formData,
                              JsonNode docweaveSnapshot,
                              UUID genAppId) {
    }
}
