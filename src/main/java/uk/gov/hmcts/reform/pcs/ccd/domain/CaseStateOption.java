package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;


/**
 * Representation of selectable case states for the Change Case State event.
 * Each constant here MUST have a matching constant of the same name in {@link State}.
 */
@Getter
@AllArgsConstructor
public enum CaseStateOption implements HasLabel {

    JUDICIAL_REFERRAL("Judicial Referral"),
    HEARING_READINESS("Hearing Readiness"),
    PREPARE_FOR_HEARING_CONDUCT_HEARING("Prepare For Hearing Conduct Hearing"),
    DECISION_OUTCOME("Decision Outcome"),
    CASE_PROGRESSION("Case Progression"),
    ALL_FINAL_ORDERS_ISSUED("All Final Orders"),
    CASE_STAYED("Case Stayed"),
    BREATHING_SPACE("Breathing space");

    private final String label;

    // Converts to the matching State constant — constant names must stay in sync with State enum
    public State toState() {
        return State.valueOf(this.name());
    }

}
