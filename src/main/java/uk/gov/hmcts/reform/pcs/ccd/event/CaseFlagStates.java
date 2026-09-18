package uk.gov.hmcts.reform.pcs.ccd.event;

import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public final class CaseFlagStates {

    public static final State[] CASE_FLAG_STATES = {
        State.PENDING_CASE_ISSUED,
        State.CASE_ISSUED,
        State.JUDICIAL_REFERRAL,
        State.HEARING_READINESS,
        State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
        State.DECISION_OUTCOME,
        State.CASE_PROGRESSION,
        State.ALL_FINAL_ORDERS_ISSUED,
        State.CASE_STAYED,
        State.BREATHING_SPACE
    };

    private CaseFlagStates() {
    }
}
