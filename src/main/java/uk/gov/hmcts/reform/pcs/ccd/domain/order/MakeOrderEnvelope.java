package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.util.List;
import java.util.UUID;

public record MakeOrderEnvelope(Action action, Order order, MakeOrderCaseContext caseContext) {

    public enum Action {
        START_DRAFT,
        SAVE_DRAFT,
        SUBMIT_FOR_REVIEW
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Order(UUID id, OrderState state, long version, MakeOrderDraftPayload draftPayload) {
    }

    public record MakeOrderCaseContext(long caseReference,
                                       AddressUK propertyAddress,
                                       List<Party> claimants,
                                       List<Party> defendants) {
    }

    public record Party(String id, String name) {
    }
}
