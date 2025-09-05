package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum NoRentArrearsDiscretionaryGrounds implements HasLabel {
    SUITABLE_ALTERNATIVE_ACCOMMODATION("Suitable alternative accommodation (ground 9)"),
    RENT_ARREARS("Rent arrears (ground 10)"),
    PERSISTENT_DELAY_IN_PAYING_RENT("Persistent delay in paying rent (ground 11)"),
    BREACH_OF_TENANCY_CONDITIONS("Breach of tenancy conditions (ground 12)"),
    PROPERTY_DETERIORATION("Deterioration in the condition of the property (ground 13)"),
    NUISANCE_OR_ILLEGAL_USE("Nuisance, annoyance, illegal or immoral use of the property (ground 14)"),
    DOMESTIC_VIOLENCE("Domestic violence (ground 14A)"),
    OFFENCE_DURING_RIOT("Offence during a riot (ground 14ZA)"),
    FURNITURE_DETERIORATION("Deterioration of furniture (ground 15)"),
    LANDLORD_EMPLOYEE("Employee of the landlord (ground 16)"),
    FALSE_STATEMENT("Tenancy obtained by false statement (ground 17)");

    private final String label;

    public static NoRentArrearsDiscretionaryGrounds fromLabel(String label) {
        return Arrays.stream(values())
            .filter(g -> g.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No enum constant with label: " + label));
    }
}
