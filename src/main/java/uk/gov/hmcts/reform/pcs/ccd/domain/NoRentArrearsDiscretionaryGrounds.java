package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@AllArgsConstructor
@Getter
public enum NoRentArrearsDiscretionaryGrounds {
    @CCD(label = "Suitable alternative accommodation (ground 9)")
    SUITABLE_ALTERNATIVE_ACCOMMODATION,

    @CCD(label = "Rent arrears (ground 10)")
    RENT_ARREARS,

    @CCD(label = "Persistent delay in paying rent (ground 11)")
    PERSISTENT_DELAY_IN_PAYING_RENT,

    @CCD(label = "Breach of tenancy conditions (ground 12)")
    BREACH_OF_TENANCY_CONDITIONS,

    @CCD(label = "Deterioration in the condition of the property (ground 13)")
    PROPERTY_DETERIORATION,

    @CCD(label = "Nuisance, annoyance, illegal or immoral use of the property (ground 14)")
    NUISANCE_OR_ILLEGAL_USE,

    @CCD(label = "Domestic violence (ground 14A)")
    DOMESTIC_VIOLENCE,

    @CCD(label = "Offence during a riot (ground 14ZA)")
    OFFENCE_DURING_RIOT,

    @CCD(label = "Deterioration of furniture (ground 15)")
    FURNITURE_DETERIORATION,

    @CCD(label = "Employee of the landlord (ground 16)")
    LANDLORD_EMPLOYEE,

    @CCD(label = "Tenancy obtained by false statement (ground 17)")
    FALSE_STATEMENT
}
