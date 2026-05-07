package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;

import java.util.Arrays;

/**
 * Enum representing the type of occupation contract or licence for Welsh properties.
 */
@AllArgsConstructor
@Getter
public enum OccupationLicenceTypeWales implements HasLabel {

    SECURE_CONTRACT("Secure contract", CombinedLicenceType.SECURE_CONTRACT),
    STANDARD_CONTRACT("Standard contract", CombinedLicenceType.STANDARD_CONTRACT),
    OTHER("Other", CombinedLicenceType.OTHER);

    private final String label;
    private final CombinedLicenceType combinedLicenceType;

    public static OccupationLicenceTypeWales from(CombinedLicenceType combinedLicenceType) {
        if (combinedLicenceType == null) {
            return null;
        }

        return Arrays.stream(values())
            .filter(value -> value.getCombinedLicenceType().equals(combinedLicenceType))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("No value found for combined licence type: " + combinedLicenceType)
            );
    }

}
