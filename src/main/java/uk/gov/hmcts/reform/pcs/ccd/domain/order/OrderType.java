package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The kinds of order a judge can make in pcs-frontend: how the orders tab and Work Allocation tasks name
 * them, and the title the issued order's document carries.
 */
@Getter
@AllArgsConstructor
public enum OrderType {
    OUTRIGHT_POSSESSION("Outright possession", "Order for possession"),
    SUSPENDED_POSSESSION("Suspended possession", "Order for possession (suspended)"),
    ADJOURNMENT("Adjournment", "Order (adjournment)"),
    STRIKE_OUT_DISMISSAL("Strike out or dismissal", "Order (strike out or dismissal)"),
    FREE_FORM("Free form", "Order");

    private final String label;
    private final String documentTitle;
}
