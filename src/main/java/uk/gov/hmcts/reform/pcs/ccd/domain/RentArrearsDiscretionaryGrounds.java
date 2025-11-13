package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Enum representing discretionary grounds for possession claims.
 */
@AllArgsConstructor
@Getter
public enum RentArrearsDiscretionaryGrounds implements HasLabel {

    @CCD(label = "Suitable alternative accommodation (ground 9)")
    ALTERNATIVE_ACCOMMODATION_GROUND9("Suitable alternative accommodation (ground 9)"),

    @CCD(label = "Rent arrears (ground 10)")
    RENT_ARREARS_GROUND10("Rent arrears (ground 10)"),

    @CCD(label = "Persistent delay in paying rent (ground 11)")
    PERSISTENT_DELAY_GROUND11("Persistent delay in paying rent (ground 11)"),

    @CCD(label = "Breach of tenancy conditions (ground 12)")
    BREACH_TENANCY_GROUND12("Breach of tenancy conditions (ground 12)"),

    @CCD(label = "Deterioration in the condition of the property (ground 13)")
    DETERIORATION_PROPERTY_GROUND13("Deterioration in the condition of the property (ground 13)"),

    @CCD(label = "Nuisance, annoyance, illegal or immoral use of the property (ground 14)")
    NUISANCE_ANNOYANCE_GROUND14("Nuisance, annoyance, illegal or immoral use of the property (ground 14)"),

    @CCD(label = "Domestic violence (ground 14A)")
    DOMESTIC_VIOLENCE_GROUND14A("Domestic violence (ground 14A)"),

    @CCD(label = "Offence during a riot (ground 14ZA)")
    OFFENCE_RIOT_GROUND14ZA("Offence during a riot (ground 14ZA)"),

    @CCD(label = "Deterioration of furniture (ground 15)")
    DETERIORATION_FURNITURE_GROUND15("Deterioration of furniture (ground 15)"),

    @CCD(label = "Employee of the landlord (ground 16)")
    EMPLOYEE_LANDLORD_GROUND16("Employee of the landlord (ground 16)"),

    @CCD(label = "Tenancy obtained by false statement (ground 17)")
    FALSE_STATEMENT_GROUND17("Tenancy obtained by false statement (ground 17)");

    private final String label;
}
