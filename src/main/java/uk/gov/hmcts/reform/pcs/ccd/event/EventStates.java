package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

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

    public static State[] addCaseNote(FeatureToggleService featureToggleService) {
        if (featureToggleService.isEnabled(RELEASE_1_DOT_3)) {
            return addCaseNote();
        }
        return new State[] {
            State.PENDING_CASE_ISSUED,
            State.CASE_ISSUED
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

    public static State[] createCaseLink(FeatureToggleService featureToggleService) {
        if (featureToggleService.isEnabled(RELEASE_1_DOT_3)) {
            return createCaseLink();
        }
        return new State[] {
            State.PENDING_CASE_ISSUED,
            State.CASE_ISSUED
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

    public static State[] createFlags(FeatureToggleService featureToggleService) {
        if (featureToggleService.isEnabled(RELEASE_1_DOT_3)) {
            return createFlags();
        }
        return new State[] {
            State.PENDING_CASE_ISSUED
        };
    }

    public static State[] makeAnApplication() {
        return createFlags();
    }

    public static State[] respondPossessionClaim() {
        return createCaseLink();
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

    public static State[] uploadDocuments(FeatureToggleService featureToggleService) {
        if (featureToggleService.isEnabled(RELEASE_1_DOT_3)) {
            return uploadDocuments();
        }
        return new State[] {
            State.CASE_ISSUED
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

    public static State[] amendFlags(FeatureToggleService featureToggleService) {
        if (featureToggleService.isEnabled(RELEASE_1_DOT_3)) {
            return amendFlags();
        }
        return new State[] {
            State.PENDING_CASE_ISSUED
        };
    }

    public static State[] maintainCaseLink() {
        return amendFlags();
    }

    public static State[] maintainCaseLink(FeatureToggleService featureToggleService) {
        if (featureToggleService.isEnabled(RELEASE_1_DOT_3)) {
            return maintainCaseLink();
        }
        return new State[] {
            State.PENDING_CASE_ISSUED,
            State.CASE_ISSUED
        };
    }
}
