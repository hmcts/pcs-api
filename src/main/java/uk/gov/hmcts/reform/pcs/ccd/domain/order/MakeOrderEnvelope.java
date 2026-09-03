package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.math.BigDecimal;
import java.time.LocalDate;
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
                                       List<Party> defendants,
                                       MakeOrderCaseFacts caseFacts) {
    }

    public record MakeOrderCaseFacts(LocalDate tenancyStartDate,
                                     String tenancyType,
                                     LocalDate noticeDate,
                                     BigDecimal currentRent,
                                     String rentFrequency,
                                     String groundsPleaded,
                                     BigDecimal arrearsOnIssue) {
    }

    public record Party(String id, String name) {
    }
}
