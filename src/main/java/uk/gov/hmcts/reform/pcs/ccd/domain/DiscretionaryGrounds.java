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
public enum DiscretionaryGrounds implements HasLabel {

    // -- Discretionary grounds
    RENT_ARREARS_OR_BREACH_OF_TENANCY("Rent arrears or breach of the tenancy (ground 1)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), false),
    NUISANCE_OR_IMMORAL_USE("Nuisance, annoyance, illegal or immoral use of the property (ground 2)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), false),
    DOMESTIC_VIOLENCE("Domestic violence (ground 2A)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), false),
    RIOT_OFFENCE("Offence during a riot (ground 22A)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), false),
    PROPERTY_DETERIORATION("Deterioration in the condition of the property (ground 3)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), false),
    FURNITURE_DETERIORATION("Deterioration of furniture (ground 4)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), false),
    TENANCY_OBTAINED_BY_FALSE_STATEMENT("Tenancy obtained by false statement (ground 5)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), false),
    PREMIUM_PAID_MUTUAL_EXCHANGE("Premium paid in connection with mutual exchange (ground 6)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), false),
    UNREASONABLE_CONDUCT_TIED_ACCOMMODATION("Unreasonable conduct in tied accommodation (ground 7)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), false),
    REFUSAL_TO_MOVE_BACK("Refusal to move back to main home after works completed (ground 8)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), false),

    // -- Discretionary grounds (alternative accommodation available)
    TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE("Tied accommodation needed for another employee (ground 12)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), true),
    ADAPTED_ACCOMMODATION("Adapted accommodation (ground 13)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), true),
    HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES("Housing association special circumstances accommodation (ground 14)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), true),
    SPECIAL_NEEDS_ACCOMMODATION("Special needs accommodation (ground 15)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), true),
    UNDER_OCCUPYING_AFTER_SUCCESSION("Under occupying after succession (ground 15A)"
        , Set.of(SECURE_TENANCY, FLEXIBLE_TENANCY), true);

    private final String label;
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

    public static DiscretionaryGrounds fromLabel(String label) {
        return Arrays.stream(values())
            .filter(g -> g.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No enum constant with label: " + label));
    }
}

