package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.extern.slf4j.Slf4j;

/**
 * Runs the side effects an enforcement change schedules — the claimant's emails, the documents to generate,
 * the Payments service request — turning a failure to schedule one into a refusal the user can read.
 *
 * <p>Each of those side effects is a row written in the event's own transaction, which is what makes it
 * atomic with the data it describes: a rollback drops it and a commit keeps both, so an enforcement can never
 * commit without its documents queued. That leaves the failure case to handle. An exception out of a submit
 * handler gives the user a 500 at the end of a journey they completed correctly, with the change rolled back
 * and nothing said about why, so it is answered as an {@link EnforcementChange} refusal instead: the handler
 * returns the error, the SDK rolls the whole event back, and the user is asked to submit again. Nothing is
 * half-done either way.</p>
 *
 * <p>This is for side effects only. A failure to write the enforcement data itself is not caught here: there
 * is nothing to tell the user beyond "it did not work", and a submit handler that carried on from one would
 * be reporting a change it had not made.</p>
 */
@Slf4j
public final class EnforcementSideEffects {

    public static final String COULD_NOT_SCHEDULE_ERROR =
        "Something went wrong, so nothing has been saved. Try submitting this again";

    private EnforcementSideEffects() {
    }

    /**
     * Schedules the side effects of a change that has otherwise been made.
     *
     * @param description what is being scheduled, for the log when it cannot be
     * @return a change that was made, or the refusal for the submit handler to answer with
     */
    public static EnforcementChange<Void> schedule(long caseReference, String description, Runnable sideEffects) {
        try {
            sideEffects.run();
            return EnforcementChange.made();
        } catch (RuntimeException e) {
            log.error("Case {}: could not schedule {}, so the whole change is being rolled back", caseReference,
                      description, e);
            return EnforcementChange.refused(COULD_NOT_SCHEDULE_ERROR);
        }
    }
}
