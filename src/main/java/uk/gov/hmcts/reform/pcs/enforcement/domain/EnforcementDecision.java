package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * The caseworker's decision on an enforcement application in the decideEnforcement event.
 */
@AllArgsConstructor
@Getter
public enum EnforcementDecision implements HasLabel {

    ACCEPT("Accept"),
    REJECT("Reject");

    private final String label;
}
