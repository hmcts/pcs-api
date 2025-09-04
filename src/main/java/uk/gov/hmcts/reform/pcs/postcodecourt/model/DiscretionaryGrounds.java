package uk.gov.hmcts.reform.pcs.postcodecourt.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.Arrays;
import java.util.Set;

@Getter
public enum DiscretionaryGrounds implements HasLabel {
    SUITABLE_ALTERNATIVE_ACCOMMODATION(
        "Suitable alternative accommodation (ground 9)",
        Set.of(TenancyLicenceType.ASSURED_TENANCY)
    ),
    RENT_ARREARS(
        "Rent arrears (ground 10)",
        Set.of(TenancyLicenceType.ASSURED_TENANCY)
    ),
    PERSISTENT_DELAY_IN_PAYING_RENT(
        "Persistent delay in paying rent (ground 11)",
        Set.of(TenancyLicenceType.ASSURED_TENANCY)
    ),
    BREACH_OF_TENANCY_CONDITIONS(
        "Breach of tenancy conditions (ground 12)",
        Set.of(TenancyLicenceType.ASSURED_TENANCY)
    ),
    PROPERTY_DETERIORATION(
        "Deterioration in the condition of the property (ground 13)",
        Set.of(TenancyLicenceType.ASSURED_TENANCY)
    ),
    NUISANCE_OR_ILLEGAL_USE(
        "Nuisance, annoyance, illegal or immoral use of the property (ground 14)",
        Set.of(TenancyLicenceType.ASSURED_TENANCY)
    ),
    DOMESTIC_VIOLENCE(
        "Domestic violence (ground 14A)",
        Set.of(TenancyLicenceType.ASSURED_TENANCY)
    ),
    OFFENCE_DURING_RIOT(
        "Offence during a riot (ground 14ZA)",
        Set.of(TenancyLicenceType.ASSURED_TENANCY)
    ),
    FURNITURE_DETERIORATION(
        "Deterioration of furniture (ground 15)",
        Set.of(TenancyLicenceType.ASSURED_TENANCY)
    ),
    LANDLORD_EMPLOYEE(
        "Employee of the landlord (ground 16)",
        Set.of(TenancyLicenceType.ASSURED_TENANCY)
    ),
    FALSE_STATEMENT(
        "Tenancy obtained by false statement (ground 17)",
        Set.of(TenancyLicenceType.ASSURED_TENANCY)
    );

    private final String label;
    private final Set<TenancyLicenceType> applicableTenancies;

    DiscretionaryGrounds(String label, Set<TenancyLicenceType> applicableTenancies) {
        this.label = label;
        this.applicableTenancies = applicableTenancies;
    }


    public static DiscretionaryGrounds fromLabel(String label) {
        if (label == null) {
            return null;
        }

        return Arrays.stream(values())
            .filter(value -> value.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No value found with label: " + label));
    }
}
