package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import java.util.UUID;

/**
 * What the link to an order event names in its client context: the order to open, the general application an order
 * decides, or the hearing an order is for. Each is absent unless the link names it. The judge's own choice of when
 * the order is made, if they made one on the page, overrides the hearing the link or the order gave it.
 */
public record OrderLink(UUID orderId, UUID genAppId, Integer hearingId, MadeAt madeAt) {

    public static final OrderLink NONE = new OrderLink(null, null, null, null);

    /** The judge's choice: at one of the case's hearings, or with no hearing id, without a hearing. */
    public record MadeAt(Integer hearingId) {
    }
}
