package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum NoRentArrearsMandatoryGrounds {

    @CCD(label = "Owner occupier (ground 1)")
    OWNER_OCCUPIER,

    @CCD(label = "Repossession by the landlord's mortgage lender (ground 2)")
    REPOSSESSION_BY_LENDER,

    @CCD(label = "Holiday let (ground 3)")
    HOLIDAY_LET,

    @CCD(label = "Student let (ground 4)")
    STUDENT_LET,

    @CCD(label = "Property required for minister of religion (ground 5)")
    MINISTER_OF_RELIGION,

    @CCD(label = "Property required for redevelopment (ground 6)")
    REDEVELOPMENT,

    @CCD(label = "Death of the tenant (ground 7)")
    DEATH_OF_TENANT,

    @CCD(label = "Antisocial behaviour (ground 7A)")
    ANTISOCIAL_BEHAVIOUR,

    @CCD(label = "Tenant does not have a right to rent (ground 7B)")
    NO_RIGHT_TO_RENT,

    @CCD(label = "Serious rent arrears (ground 8)")
    SERIOUS_RENT_ARREARS
}

