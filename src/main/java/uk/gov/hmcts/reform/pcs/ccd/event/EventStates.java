package uk.gov.hmcts.reform.pcs.ccd.event;

import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public class EventStates {

    public static State[] claimantMakeAnApplication() {
        return new State[] {
            State.CASE_ISSUED,
            State.CASE_PROGRESSION,
            State.CASE_STAYED,
            State.BREATHING_SPACE,
            State.JUDICIAL_REFERRAL,
            State.HEARING_READINESS,
            State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
            State.DECISION_OUTCOME,
            State.ALL_FINAL_ORDERS_ISSUED,
            State.CLOSED
        };
    }
}
