package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import java.util.UUID;

/**
 * What the link to an order event names in its client context: the order to open, the general application an order
 * decides, or the hearing an order is for. Each is absent unless the link names it.
 */
public record OrderLink(UUID orderId, UUID genAppId, Integer hearingId) {

    public static final OrderLink NONE = new OrderLink(null, null, null);
}
