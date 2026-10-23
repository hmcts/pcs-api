package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** The kinds of order a judge can make, as pcs-frontend names them. */
@Getter
@AllArgsConstructor
public enum OrderType {
    OUTRIGHT_POSSESSION("Outright possession", "Order for possession"),
    SUSPENDED_POSSESSION("Suspended possession", "Order for possession (suspended)"),
    ADJOURNMENT("Adjournment", "Order (adjournment)"),
    STRIKE_OUT_DISMISSAL("Strike out or dismissal", "Order (strike out or dismissal)"),
    FREE_FORM("Free form", "Order"),
    NO_ORDER_ON_CLAIM("No order on the claim", "Order");

    private final String label;
    private final String documentTitle;
}
