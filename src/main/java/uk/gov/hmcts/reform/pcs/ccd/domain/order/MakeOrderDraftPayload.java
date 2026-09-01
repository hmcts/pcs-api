package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MakeOrderDraftPayload(int version,
                                    OrderType orderType,
                                    JsonNode fields,
                                    Map<OrderType, JsonNode> documents) {

    public enum OrderType {
        OUTRIGHT_POSSESSION,
        SUSPENDED_POSSESSION,
        ADJOURNMENT,
        STRIKE_OUT_DISMISSAL,
        FREE_FORM
    }
}
