package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.CCD;

/**
 * Enum representing mandatory grounds for possession claims.
 */
public enum MandatoryGround {

    @CCD(label = "Owner occupier (ground 1)")
    OWNER_OCCUPIER_GROUND1,

    @CCD(label = "Repossession by the landlord's mortgage lender (ground 2)")
    REPOSSESSION_GROUND2,

    @CCD(label = "Holiday let (ground 3)")
    HOLIDAY_LET_GROUND3,

    @CCD(label = "Student let (ground 4)")
    STUDENT_LET_GROUND4,

    @CCD(label = "Property required for minister of religion (ground 5)")
    MINISTER_RELIGION_GROUND5,

    @CCD(label = "Property required for redevelopment (ground 6)")
    REDEVELOPMENT_GROUND6,

    @CCD(label = "Death of the tenant (ground 7)")
    DEATH_OF_TENANT_GROUND7,

    @CCD(label = "Antisocial behaviour (ground 7A)")
    ANTISOCIAL_BEHAVIOUR_GROUND7A,

    @CCD(label = "Tenant does not have a right to rent (ground 7B)")
    NO_RIGHT_TO_RENT_GROUND7B,

    @CCD(label = "Serious rent arrears (ground 8)")
    SERIOUS_RENT_ARREARS_GROUND8;
}
