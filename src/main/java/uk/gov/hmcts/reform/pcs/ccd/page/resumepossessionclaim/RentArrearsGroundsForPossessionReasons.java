package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundReasons;

public class RentArrearsGroundsForPossessionReasons implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("rentArrearsGroundsForPossessionReasons")
            .pageLabel("Grounds for possession")
            .showCondition("groundsForPossession=\"Yes\""
                               + " AND typeOfTenancyLicence=\"ASSURED_TENANCY\""
                               + " AND showRentArrearsReasonsForGroundsPage=\"Yes\"")
            .complex(PCSCase::getRentArrearsGroundReasons)

            // ---------- Mandatory grounds ----------
            .label("rentArrears-ownerOccupier-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Owner occupier (ground 1)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"OWNER_OCCUPIER_GROUND1\"")
            .mandatory(
                RentArrearsGroundReasons::getOwnerOccupierGround,
                "rentArrearsMandatoryGroundsCONTAINS\"OWNER_OCCUPIER_GROUND1\"")

            .label("rentArrears-repossessionByLender-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Repossession by the landlord's mortgage lender (ground 2)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"REPOSSESSION_GROUND2\"")
            .mandatory(
                RentArrearsGroundReasons::getRepossessionByLenderGround,
                "rentArrearsMandatoryGroundsCONTAINS\"REPOSSESSION_GROUND2\"")

            .label("rentArrears-holidayLet-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Holiday let (ground 3)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"HOLIDAY_LET_GROUND3\"")
            .mandatory(
                RentArrearsGroundReasons::getHolidayLetGround,
                "rentArrearsMandatoryGroundsCONTAINS\"HOLIDAY_LET_GROUND3\"")

            .label("rentArrears-studentLet-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Student let (ground 4)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"STUDENT_LET_GROUND4\"")
            .mandatory(
                RentArrearsGroundReasons::getStudentLetGround,
                "rentArrearsMandatoryGroundsCONTAINS\"STUDENT_LET_GROUND4\"")

            .label("rentArrears-ministerOfReligion-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Property required for minister of religion (ground 5)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"MINISTER_RELIGION_GROUND5\"")
            .mandatory(
                RentArrearsGroundReasons::getMinisterOfReligionGround,
                "rentArrearsMandatoryGroundsCONTAINS\"MINISTER_RELIGION_GROUND5\"")

            .label("rentArrears-redevelopment-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Property required for redevelopment (ground 6)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"REDEVELOPMENT_GROUND6\"")
            .mandatory(
                RentArrearsGroundReasons::getRedevelopmentGround,
                "rentArrearsMandatoryGroundsCONTAINS\"REDEVELOPMENT_GROUND6\"")

            .label("rentArrears-deathOfTenant-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Death of the tenant (ground 7)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"DEATH_OF_TENANT_GROUND7\"")
            .mandatory(
                RentArrearsGroundReasons::getDeathOfTenantGround,
                "rentArrearsMandatoryGroundsCONTAINS\"DEATH_OF_TENANT_GROUND7\"")

            .label("rentArrears-antisocialBehaviour-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Antisocial behaviour (ground 7A)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"ANTISOCIAL_BEHAVIOUR_GROUND7A\"")
            .mandatory(
                RentArrearsGroundReasons::getAntisocialBehaviourGround,
                "rentArrearsMandatoryGroundsCONTAINS\"ANTISOCIAL_BEHAVIOUR_GROUND7A\"")

            .label("rentArrears-noRightToRent-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Tenant does not have a right to rent (ground 7B)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"NO_RIGHT_TO_RENT_GROUND7B\"")
            .mandatory(
                RentArrearsGroundReasons::getNoRightToRentGround,
                "rentArrearsMandatoryGroundsCONTAINS\"NO_RIGHT_TO_RENT_GROUND7B\"")

            // ---------- Discretionary grounds ----------
            .label("rentArrears-suitableAlternativeAccommodation-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Suitable alternative accommodation (ground 9)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"ALTERNATIVE_ACCOMMODATION_GROUND9\"")
            .mandatory(
                RentArrearsGroundReasons::getSuitableAltAccommodationGround,
                "rentArrearsDiscretionaryGroundsCONTAINS\"ALTERNATIVE_ACCOMMODATION_GROUND9\"")

            .label("rentArrears-breachOfTenancyConditions-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Breach of tenancy conditions (ground 12)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"BREACH_TENANCY_GROUND12\"")
            .mandatory(
                RentArrearsGroundReasons::getBreachOfTenancyConditionsGround,
                "rentArrearsDiscretionaryGroundsCONTAINS\"BREACH_TENANCY_GROUND12\"")

            .label("rentArrears-propertyDeterioration-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Deterioration in the condition of the property (ground 13)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"DETERIORATION_PROPERTY_GROUND13\"")
            .mandatory(
                RentArrearsGroundReasons::getPropertyDeteriorationGround,
                "rentArrearsDiscretionaryGroundsCONTAINS\"DETERIORATION_PROPERTY_GROUND13\"")

            .label("rentArrears-nuisanceAnnoyance-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Nuisance, annoyance, illegal or immoral use of the property (ground 14)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"NUISANCE_ANNOYANCE_GROUND14\"")
            .mandatory(
                RentArrearsGroundReasons::getNuisanceAnnoyanceGround,
                "rentArrearsDiscretionaryGroundsCONTAINS\"NUISANCE_ANNOYANCE_GROUND14\"")

            .label("rentArrears-domesticViolence-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Domestic violence (ground 14A)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE_GROUND14A\"")
            .mandatory(
                RentArrearsGroundReasons::getDomesticViolenceGround,
                "rentArrearsDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE_GROUND14A\"")

            .label("rentArrears-offenceDuringRiot-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Offence during a riot (ground 14ZA)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"OFFENCE_RIOT_GROUND14ZA\"")
            .mandatory(
                RentArrearsGroundReasons::getOffenceDuringRiotGround,
                "rentArrearsDiscretionaryGroundsCONTAINS\"OFFENCE_RIOT_GROUND14ZA\"")

            .label("rentArrears-furnitureDeterioration-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Deterioration of furniture (ground 15)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"DETERIORATION_FURNITURE_GROUND15\"")
            .mandatory(
                RentArrearsGroundReasons::getFurnitureDeteriorationGround,
                "rentArrearsDiscretionaryGroundsCONTAINS\"DETERIORATION_FURNITURE_GROUND15\"")

            .label("rentArrears-employeeOfLandlord-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Employee of the landlord (ground 16)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"EMPLOYEE_LANDLORD_GROUND16\"")
            .mandatory(
                RentArrearsGroundReasons::getEmployeeOfLandlordGround,
                "rentArrearsDiscretionaryGroundsCONTAINS\"EMPLOYEE_LANDLORD_GROUND16\"")

            .label("rentArrears-tenancyByFalseStatement-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Tenancy obtained by false statement (ground 17)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"FALSE_STATEMENT_GROUND17\"")
            .mandatory(
                RentArrearsGroundReasons::getTenancyByFalseStatementGround,
                "rentArrearsDiscretionaryGroundsCONTAINS\"FALSE_STATEMENT_GROUND17\"")
            .done();
    }
}
