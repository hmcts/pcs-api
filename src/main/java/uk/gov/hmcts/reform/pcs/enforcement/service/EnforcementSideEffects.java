package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.extern.slf4j.Slf4j;

/**
 * Turns a failure to schedule an enforcement change's side effects into a refusal, so the SDK rolls the event
 * back instead of the user getting a 500. A failure to write the enforcement data itself is not caught here.
 */
@Slf4j
public final class EnforcementSideEffects {

    public static final String COULD_NOT_SCHEDULE_ERROR =
        "Something went wrong, so nothing has been saved. Try submitting this again";

    private EnforcementSideEffects() {
    }

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
