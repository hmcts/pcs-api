package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@UtilityClass
public class EventStates {

    public static State[] addCaseNote() {
        return new State[] {
            State.PENDING_CASE_ISSUED,
            State.CASE_ISSUED,
            State.CASE_PROGRESSION,
            State.CASE_STAYED,
            State.BREATHING_SPACE,
            State.JUDICIAL_REFERRAL,
            State.HEARING_READINESS,
            State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
            State.DECISION_OUTCOME
        };
    }

    public static State[] enterCounterClaim() {
        return new State[] {
            State.CASE_ISSUED,
            State.CASE_PROGRESSION,
            State.CASE_STAYED,
            State.BREATHING_SPACE,
            State.JUDICIAL_REFERRAL,
            State.HEARING_READINESS,
            State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
            State.DECISION_OUTCOME,
            State.ALL_FINAL_ORDERS_ISSUED
        };
    }

    public static State[] createCaseLink() {
        return new State[] {
            State.CASE_ISSUED,
            State.CASE_PROGRESSION,
            State.CASE_STAYED,
            State.BREATHING_SPACE,
            State.JUDICIAL_REFERRAL,
            State.HEARING_READINESS,
            State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
            State.DECISION_OUTCOME,
            State.ALL_FINAL_ORDERS_ISSUED
        };
    }

    public static State[] createFlags() {
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

    public static State[] makeAnApplication() {
        return createFlags();
    }

    public static State[] respondPossessionClaim() {
        return createCaseLink();
    }

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

    public static State[] resumePossessionClaim() {
        return new State[] {
            State.AWAITING_SUBMISSION_TO_HMCTS
        };
    }

    public static State[] uploadDocuments() {
        return new State[] {
            State.AWAITING_RESUBMISSION_TO_HMCTS,
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

    public static State[] amendFlags() {
        return new State[] {
            State.PENDING_CASE_ISSUED,
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

    public static State[] paperResponseDefendant() {
        return createFlags();
    }

    public static State[] maintainCaseLink() {
        return amendFlags();
    }
}
