package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

/**
 * What the frontend sends when a judge acts on their order: the action, the order as the judge last
 * saw it, naming the version the change is made from, and the Work Allocation task they came from, if
 * they are changing an order returned to them.
 */
public record MakeOrderRequest(Action action, OrderChange order, String taskId) {

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
}
