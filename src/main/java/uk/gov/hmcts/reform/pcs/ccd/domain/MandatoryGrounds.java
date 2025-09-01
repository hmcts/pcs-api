package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.FLEXIBLE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.SECURE_TENANCY;

@AllArgsConstructor
@Getter
public enum MandatoryGrounds implements HasLabel {

    ANTI_SOCIAL("Antisocial behaviour", Set.of(SECURE_TENANCY,FLEXIBLE_TENANCY), false),
    OVERCROWDING("Overcrowding(ground 9)", Set.of(SECURE_TENANCY,FLEXIBLE_TENANCY),true),
    LANDLORD_WORKS("Landlord's works(ground 10)",Set.of(SECURE_TENANCY,FLEXIBLE_TENANCY), true),
    PROPERTY_SOLD("Property sold for redevelopment(ground 10A)",Set.of(SECURE_TENANCY,FLEXIBLE_TENANCY), true),
    CHARITABLE_LANDLORD("Charitable landlords(ground 11)",Set.of(SECURE_TENANCY,FLEXIBLE_TENANCY), true);

    private String label;
    private final Set<TenancyLicenceType> tenancyLicenceTypeSet;
    private final boolean alternativeAccommodationAvailable;


    public boolean isApplicableFor(TenancyLicenceType tenancyLicenceType) {
        return tenancyLicenceTypeSet.contains(tenancyLicenceType);
    }

    public Set<TenancyLicenceType> getTenancyLicenceTypeSet() {
        return tenancyLicenceTypeSet;
    }

    public boolean isAlternativeAccommodationAvailable() {
        return alternativeAccommodationAvailable;
    }


    public static MandatoryGrounds fromLabel(String label) {
        return Arrays.stream(values())
                .filter(g -> g.getLabel().equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No enum constant with label: " + label));
    }
}
