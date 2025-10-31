package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsReasons;

public class RentArrearsGroundsForPossessionReasons implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("rentArrearsGroundsForPossessionReasons")
            .pageLabel("Reasons for possession")
            .showCondition("groundsForPossession=\"Yes\""
                               + " AND typeOfTenancyLicence=\"ASSURED_TENANCY\""
                               + " AND showRentArrearsGroundReasonPage=\"Yes\""
                               + " AND hasOtherAdditionalGrounds=\"Yes\""
                               + " AND legislativeCountry=\"England\""
            )
            .label("rentArrearsGrounds-lineSeparator","---")
            .complex(PCSCase::getRentArrearsGroundsReasons)

            // ---------- Mandatory grounds ----------
            .label("rentArrears-ownerOccupier-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Owner occupier (ground 1)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"OWNER_OCCUPIER_GROUND1\"")
            .mandatory(RentArrearsGroundsReasons::getOwnerOccupierReason,
                "rentArrearsMandatoryGroundsCONTAINS\"OWNER_OCCUPIER_GROUND1\"")

            .label("rentArrears-repossessionByLender-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Repossession by the landlord's mortgage lender (ground 2)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"REPOSSESSION_GROUND2\"")
            .mandatory(RentArrearsGroundsReasons::getRepossessionByLenderReason,
                "rentArrearsMandatoryGroundsCONTAINS\"REPOSSESSION_GROUND2\"")

            .label("rentArrears-holidayLet-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Holiday let (ground 3)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"HOLIDAY_LET_GROUND3\"")
            .mandatory(RentArrearsGroundsReasons::getHolidayLetReason,
                "rentArrearsMandatoryGroundsCONTAINS\"HOLIDAY_LET_GROUND3\"")

            .label("rentArrears-studentLet-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Student let (ground 4)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"STUDENT_LET_GROUND4\"")
            .mandatory(RentArrearsGroundsReasons::getStudentLetReason,
                "rentArrearsMandatoryGroundsCONTAINS\"STUDENT_LET_GROUND4\"")

            .label("rentArrears-ministerOfReligion-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Property required for minister of religion (ground 5)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"MINISTER_RELIGION_GROUND5\"")
            .mandatory(RentArrearsGroundsReasons::getMinisterOfReligionReason,
                "rentArrearsMandatoryGroundsCONTAINS\"MINISTER_RELIGION_GROUND5\"")

            .label("rentArrears-redevelopment-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Property required for redevelopment (ground 6)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"REDEVELOPMENT_GROUND6\"")
            .mandatory(RentArrearsGroundsReasons::getRedevelopmentReason,
                "rentArrearsMandatoryGroundsCONTAINS\"REDEVELOPMENT_GROUND6\"")

            .label("rentArrears-deathOfTenant-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Death of the tenant (ground 7)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"DEATH_OF_TENANT_GROUND7\"")
            .mandatory(RentArrearsGroundsReasons::getDeathOfTenantReason,
                "rentArrearsMandatoryGroundsCONTAINS\"DEATH_OF_TENANT_GROUND7\"")

            .label("rentArrears-antisocialBehaviour-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Antisocial behaviour (ground 7A)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"ANTISOCIAL_BEHAVIOUR_GROUND7A\"")
            .mandatory(RentArrearsGroundsReasons::getAntisocialBehaviourReason,
                "rentArrearsMandatoryGroundsCONTAINS\"ANTISOCIAL_BEHAVIOUR_GROUND7A\"")

            .label("rentArrears-noRightToRent-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Tenant does not have a right to rent (ground 7B)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsMandatoryGroundsCONTAINS\"NO_RIGHT_TO_RENT_GROUND7B\"")
            .mandatory(RentArrearsGroundsReasons::getNoRightToRentReason,
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
            .mandatory(RentArrearsGroundsReasons::getSuitableAltAccommodationReason,
                "rentArrearsDiscretionaryGroundsCONTAINS\"ALTERNATIVE_ACCOMMODATION_GROUND9\"")

            .label("rentArrears-breachOfTenancyConditions-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Breach of tenancy conditions (ground 12)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"BREACH_TENANCY_GROUND12\"")
            .mandatory(RentArrearsGroundsReasons::getBreachOfTenancyConditionsReason,
                "rentArrearsDiscretionaryGroundsCONTAINS\"BREACH_TENANCY_GROUND12\"")

            .label("rentArrears-propertyDeterioration-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Deterioration in the condition of the property (ground 13)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"DETERIORATION_PROPERTY_GROUND13\"")
            .mandatory(RentArrearsGroundsReasons::getPropertyDeteriorationReason,
                "rentArrearsDiscretionaryGroundsCONTAINS\"DETERIORATION_PROPERTY_GROUND13\"")

            .label("rentArrears-nuisanceAnnoyance-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Nuisance, annoyance, illegal or immoral use of the property (ground 14)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"NUISANCE_ANNOYANCE_GROUND14\"")
            .mandatory(RentArrearsGroundsReasons::getNuisanceAnnoyanceReason,
                "rentArrearsDiscretionaryGroundsCONTAINS\"NUISANCE_ANNOYANCE_GROUND14\"")

            .label("rentArrears-domesticViolence-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Domestic violence (ground 14A)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE_GROUND14A\"")
            .mandatory(RentArrearsGroundsReasons::getDomesticViolenceReason,
                "rentArrearsDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE_GROUND14A\"")

            .label("rentArrears-offenceDuringRiot-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Offence during a riot (ground 14ZA)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"OFFENCE_RIOT_GROUND14ZA\"")
            .mandatory(RentArrearsGroundsReasons::getOffenceDuringRiotReason,
                "rentArrearsDiscretionaryGroundsCONTAINS\"OFFENCE_RIOT_GROUND14ZA\"")

            .label("rentArrears-furnitureDeterioration-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Deterioration of furniture (ground 15)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"DETERIORATION_FURNITURE_GROUND15\"")
            .mandatory(RentArrearsGroundsReasons::getFurnitureDeteriorationReason,
                "rentArrearsDiscretionaryGroundsCONTAINS\"DETERIORATION_FURNITURE_GROUND15\"")

            .label("rentArrears-employeeOfLandlord-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Employee of the landlord (ground 16)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"EMPLOYEE_LANDLORD_GROUND16\"")
            .mandatory(RentArrearsGroundsReasons::getEmployeeOfLandlordReason,
                "rentArrearsDiscretionaryGroundsCONTAINS\"EMPLOYEE_LANDLORD_GROUND16\"")

            .label("rentArrears-tenancyByFalseStatement-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Tenancy obtained by false statement (ground 17)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "rentArrearsDiscretionaryGroundsCONTAINS\"FALSE_STATEMENT_GROUND17\"")
            .mandatory(RentArrearsGroundsReasons::getTenancyByFalseStatementReason,
                "rentArrearsDiscretionaryGroundsCONTAINS\"FALSE_STATEMENT_GROUND17\"")
            .done();
    }
}
