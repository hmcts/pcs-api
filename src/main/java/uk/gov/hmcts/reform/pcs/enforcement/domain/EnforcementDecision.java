package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum EnforcementDecision implements HasLabel {

    ACCEPT("Accept"),
    REJECT("Reject");

    private final String label;
}
