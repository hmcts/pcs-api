package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public final class ReviewDateStates {

    public static final State[] REVIEW_DATE_STATES = {
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

    private ReviewDateStates() {
    }
}
