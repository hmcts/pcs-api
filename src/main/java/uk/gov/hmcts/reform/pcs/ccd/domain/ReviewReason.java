package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum ReviewReason implements HasLabel {

    UNLESS_ORDER("Unless order"),
    STAY_CASE("Stay a case"),
    LIFT_STAY("Lift a stay"),
    DISMISS_CASE("Dismiss case"),
    GENERAL_ORDER("General order"),
    OTHER("Other");

    private final String label;
}
