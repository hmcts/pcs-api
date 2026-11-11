package uk.gov.hmcts.reform.pcs.ccd.domain.order;

/** What an order decides about a general application. */
public enum GenAppDecision {

    GRANT,
    DISMISS,
    STRIKE_OUT,
    /** Listed for a hearing on notice, which leaves the application to be decided. */
    LIST,
    /** The judge's own wording, which leaves the application open for another order to decide. */
    OTHER
}
