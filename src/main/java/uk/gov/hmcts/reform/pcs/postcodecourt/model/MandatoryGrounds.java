package uk.gov.hmcts.reform.pcs.postcodecourt.model;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.Arrays;
import java.util.Set;

@Getter
public enum MandatoryGrounds implements HasLabel {

    OWNER_OCCUPIER("Owner occupier (ground 1)", Set.of(TenancyLicenceType.ASSURED_TENANCY)),
    REPOSSESSION_BY_LENDER("Repossession by the landlord's mortgage lender (ground 2)",
                           Set.of(TenancyLicenceType.ASSURED_TENANCY)),
    HOLIDAY_LET("Holiday let (ground 3)", Set.of(TenancyLicenceType.ASSURED_TENANCY)),
    STUDENT_LET("Student let (ground 4)", Set.of(TenancyLicenceType.ASSURED_TENANCY)),
    MINISTER_OF_RELIGION("Property required for minister of religion (ground 5)",
                         Set.of(TenancyLicenceType.ASSURED_TENANCY)),
    REDEVELOPMENT("Property required for redevelopment (ground 6)",
                  Set.of(TenancyLicenceType.ASSURED_TENANCY)),
    DEATH_OF_TENANT("Death of the tenant (ground 7)", Set.of(TenancyLicenceType.ASSURED_TENANCY)),
    ANTISOCIAL_BEHAVIOUR("Antisocial behaviour (ground 7A)", Set.of(TenancyLicenceType.ASSURED_TENANCY)),
    NO_RIGHT_TO_RENT("Tenant does not have a right to rent (ground 7B)",
                     Set.of(TenancyLicenceType.ASSURED_TENANCY)),
    SERIOUS_RENT_ARREARS("Serious rent arrears (ground 8)", Set.of(TenancyLicenceType.ASSURED_TENANCY));

    private final String label;
    private final Set<TenancyLicenceType> applicableTenancies;

    MandatoryGrounds(String label, Set<TenancyLicenceType> applicableTenancies) {
        this.label = label;
        this.applicableTenancies = applicableTenancies;
    }

    public static MandatoryGrounds fromLabel(String label) {
        if (label == null) {
            return null;
        }

        return Arrays.stream(values())
            .filter(value -> value.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No value found with label: " + label));
    }
}
