package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum RiskLevel implements HasLabel {

    HIGH("High"),
    MEDIUM("Medium");

    private final String label;
}
