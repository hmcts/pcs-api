package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

/** What a judge's order decided about a general application. Listing it for a hearing leaves it open. */
public enum ApplicationOutcome {
    GRANTED,
    REFUSED,
    STRUCK_OUT,
    LISTED,
    OTHER
}
