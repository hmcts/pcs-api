package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.breathingspace;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
@AllArgsConstructor
public enum BreathingSpaceType implements HasLabel {

    MENTAL_HEALTH_CRISIS_MORATORIUM("Mental Health Crisis Moratorium"),
    STANDARD("Standard Breathing Space");

    private final String label;

}
