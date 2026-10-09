package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;

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

    /**
     * The order; for a judge's working order, no id means they have no draft on the case yet. The
     * version is the id of the step the order was last left by, sent back with a change to it.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Order(UUID id,
                        OrderState state,
                        long version,
                        String orderType,
                        Map<String, Object> formData,
                        JsonNode docweaveSnapshot,
                        String queryFromCaseworker) {

        public static final Order NONE = new Order(null, OrderState.DRAFT, 0, null, Map.of(), null, null);

        /** An order as its latest step left it, with the query it was returned with if it was. */
        public static Order of(OrderEntity order, OrderStepEntity latest, String queryFromCaseworker) {
            return new Order(order.getId(), order.getState(), latest.getId(), latest.getOrderType(),
                latest.getFormData(), latest.getDocweaveSnapshot(), queryFromCaseworker);
        }
    }

    /** Open means issued: nothing yet closes a counterclaim or an application. */
    public record CaseContext(long caseReference,
                              AddressUK propertyAddress,
                              List<Party> claimants,
                              List<Party> defendants,
                              CaseFacts caseFacts,
                              boolean openCounterclaim,
                              boolean openApplication) {
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
