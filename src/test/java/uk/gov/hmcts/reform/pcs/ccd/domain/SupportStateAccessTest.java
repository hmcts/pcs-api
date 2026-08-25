package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ExternalCaseFlagAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ExternalCaseFlagReadAccess;
import uk.gov.hmcts.reform.pcs.ccd.event.CaseFlagStates;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupportStateAccessTest {

    private static final List<State> SUPPORT_TAB_STATES = List.of(
        State.PENDING_CASE_ISSUED,
        State.CASE_ISSUED,
        State.JUDICIAL_REFERRAL,
        State.HEARING_READINESS,
        State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
        State.DECISION_OUTCOME,
        State.CASE_PROGRESSION,
        State.ALL_FINAL_ORDERS_ISSUED,
        State.CASE_STAYED,
        State.BREATHING_SPACE,
        State.CLOSED
    );

    @Test
    void shouldGrantExternalReadOnEveryStateSupportIsAvailableIn() {
        assertThat(SUPPORT_TAB_STATES)
            .allSatisfy(state -> assertThat(externalRolesCanRead(state))
                .as("external read access for %s", state)
                .isTrue());
    }

    @Test
    void shouldGrantExternalReadOnClosedWithoutEnablingSupportEvents() {
        assertThat(accessClasses(State.CLOSED)).contains(ExternalCaseFlagReadAccess.class);
        assertThat(CaseFlagStates.CASE_FLAG_STATES).doesNotContain(State.CLOSED);
    }

    @Test
    void shouldNotGrantExternalReadBeforeACaseIsSubmitted() {
        assertThat(externalRolesCanRead(State.AWAITING_SUBMISSION_TO_HMCTS)).isFalse();
        assertThat(externalRolesCanRead(State.AWAITING_RESUBMISSION_TO_HMCTS)).isFalse();
    }

    private boolean externalRolesCanRead(State state) {
        List<Class<? extends HasAccessControl>> accessClasses = accessClasses(state);

        return accessClasses.contains(ExternalCaseFlagReadAccess.class)
            || accessClasses.contains(ExternalCaseFlagAccess.class);
    }

    private List<Class<? extends HasAccessControl>> accessClasses(State state) {
        CCD ccd = stateField(state).getAnnotation(CCD.class);

        return ccd == null ? List.of() : Arrays.asList(ccd.access());
    }

    private Field stateField(State state) {
        try {
            return State.class.getField(state.name());
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
