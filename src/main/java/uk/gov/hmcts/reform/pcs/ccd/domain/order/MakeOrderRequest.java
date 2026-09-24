package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * What the frontend sends when a judge acts on their order: the action, and the order as the judge
 * last saw it, naming the version the change is made from.
 */
public record MakeOrderRequest(Action action, OrderChange order) {

    public enum Action {
        START_DRAFT,
        SAVE_DRAFT,
        SUBMIT_FOR_REVIEW
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderChange(UUID id, long version, JsonNode draftPayload) {
    }
}
