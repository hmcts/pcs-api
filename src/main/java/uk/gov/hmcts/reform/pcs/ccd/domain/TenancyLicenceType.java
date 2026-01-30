package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum TenancyLicenceType implements HasLabel {

    ASSURED_TENANCY("Assured tenancy", CombinedLicenceType.ASSURED_TENANCY),
    SECURE_TENANCY("Secure tenancy", CombinedLicenceType.SECURE_TENANCY),
    INTRODUCTORY_TENANCY("Introductory tenancy", CombinedLicenceType.INTRODUCTORY_TENANCY),
    FLEXIBLE_TENANCY("Flexible tenancy", CombinedLicenceType.FLEXIBLE_TENANCY),
    DEMOTED_TENANCY("Demoted tenancy", CombinedLicenceType.DEMOTED_TENANCY),
    OTHER("Other", CombinedLicenceType.OTHER);

    private final String label;
    private final CombinedLicenceType combinedLicenceType;

    public static TenancyLicenceType from(CombinedLicenceType combinedLicenceType) {
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
