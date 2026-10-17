package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * The eviction risk level a bailiff sets from the claimant's risk assessment (HDPI-7343).
 */
@AllArgsConstructor
@Getter
public enum RiskLevel implements HasLabel {

    HIGH("High"),
    MEDIUM("Medium");

    private final String label;
}
