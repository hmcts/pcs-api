package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** What the frontend shows a judge when they open the make order event: their working order and the case. */
public record MakeOrderStart(Order order, CaseContext caseContext) {

    /** The judge's working order; with no id, they have no draft on the case yet. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Order(UUID id,
                        DraftOrderState state,
                        long version,
                        String orderType,
                        Map<String, Object> formData,
                        JsonNode docweaveSnapshot) {
    }

    public record CaseContext(long caseReference,
                              AddressUK propertyAddress,
                              List<Party> claimants,
                              List<Party> defendants,
                              CaseFacts caseFacts) {
    }

    public record CaseFacts(LocalDate tenancyStartDate,
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
