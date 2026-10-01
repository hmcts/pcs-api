package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the frontend is sent when an order event starts: the order, and the case it is made on. The make order
 * event sends the judge their working order, or the order a caseworker returned to them with its query; the
 * confirm order review event sends the caseworker the order awaiting review.
 */
public record OrderStart(Order order, CaseContext caseContext) {

    /** The order; for a judge's working order, no id means they have no draft on the case yet. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Order(UUID id,
                        DraftOrderState state,
                        long version,
                        String orderType,
                        Map<String, Object> formData,
                        JsonNode docweaveSnapshot,
                        String queryFromCaseworker) {

        public static Order of(DraftOrderEntity order) {
            return of(order, null);
        }

        /** An order a caseworker returned to the judge, with the query they returned it with. */
        public static Order of(DraftOrderEntity order, String queryFromCaseworker) {
            return new Order(order.getId(), order.getState(), order.getVersion(),
                order.getOrderType(), order.getFormData(), order.getDocweaveSnapshot(), queryFromCaseworker);
        }
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
                            String groundsPleaded) {
    }

    public record Party(String id, String name) {
    }
}
