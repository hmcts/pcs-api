package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enforcement side effects are scheduled, or refused rather than thrown")
class EnforcementSideEffectsTest {

    private static final long CASE_REFERENCE = 1234123412341234L;

    @Test
    void shouldReportAChangeWhoseSideEffectsWereScheduled() {
        EnforcementChange<Void> scheduled =
            EnforcementSideEffects.schedule(CASE_REFERENCE, "the claimant's email", () -> { });

        assertThat(scheduled.refused()).isFalse();
        assertThat(scheduled.errors()).isEmpty();
    }

    /**
     * Thrown out of a submit handler this is a 500 at the end of a journey the user completed; returned as a
     * refusal it is an error they can act on, and the SDK rolls the whole event back either way.
     */
    @Test
    void shouldRefuseTheChangeWhenASideEffectCannotBeScheduled() {
        EnforcementChange<Void> scheduled = EnforcementSideEffects.schedule(
            CASE_REFERENCE, "the claimant's email",
            () -> {
                throw new DataIntegrityViolationException("case_notification");
            });

        assertThat(scheduled.errors()).containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
        assertThat(scheduled.change()).isNull();
    }
}
