package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Enum representing the type of occupation contract or licence for Welsh properties.
 */
@AllArgsConstructor
@Getter
public enum OccupationLicenceTypeWales implements HasLabel {

    SECURE_CONTRACT("Secure contract"),
    STANDARD_CONTRACT("Standard contract"),
    OTHER("Other");

    private final String label;
}

