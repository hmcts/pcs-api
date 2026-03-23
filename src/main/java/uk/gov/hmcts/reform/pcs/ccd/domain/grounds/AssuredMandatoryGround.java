package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;

/**
 * Mandatory grounds for assured tenancy possession claims.
 */
@AllArgsConstructor
@Getter
public enum AssuredMandatoryGround implements PossessionGroundEnum {

    OWNER_OCCUPIER_GROUND1("Owner occupier (ground 1)"),
    REPOSSESSION_GROUND2("Repossession by the landlord’s mortgage lender (ground 2)"),
    HOLIDAY_LET_GROUND3("Holiday let (ground 3)"),
    STUDENT_LET_GROUND4("Student let (ground 4)"),
    MINISTER_RELIGION_GROUND5("Property required for minister of religion (ground 5)"),
    REDEVELOPMENT_GROUND6("Property required for redevelopment (ground 6)"),
    DEATH_OF_TENANT_GROUND7("Death of the tenant (ground 7)"),
    ANTISOCIAL_BEHAVIOUR_GROUND7A("Antisocial behaviour (ground 7A)"),
    NO_RIGHT_TO_RENT_GROUND7B("Tenant does not have a right to rent (ground 7B)"),
    SERIOUS_RENT_ARREARS_GROUND8("Serious rent arrears (ground 8)");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
