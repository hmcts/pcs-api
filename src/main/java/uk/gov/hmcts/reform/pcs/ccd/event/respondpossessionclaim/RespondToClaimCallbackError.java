package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

/**
 * Error codes returned in callback responses for the respond-to-claim event. pcs-frontend maps them to user-facing
 * text.
 */
public final class RespondToClaimCallbackError {

    public static final String DRAFT_CHANGED = "DRAFT_CHANGED";

    private RespondToClaimCallbackError() {
    }
}
