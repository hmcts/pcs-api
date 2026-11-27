package uk.gov.hmcts.reform.pcs.ccd.domain.order;

/**
 * What an order does with a general application it deals with. The order's wording says what was decided; this only
 * says whether the application is closed or still to be decided at a hearing.
 */
public enum GenAppDecision {

    /** Dealt with, so closed: no other order decides it. */
    DISPOSED,
    /** Listed for a hearing, which leaves the application to be decided. */
    LISTED
}
