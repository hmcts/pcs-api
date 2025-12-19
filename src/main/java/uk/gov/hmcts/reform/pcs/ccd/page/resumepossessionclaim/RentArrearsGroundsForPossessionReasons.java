package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class RentArrearsGroundsForPossessionReasons implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("rentArrearsGroundsForPossessionReasons", this::midEvent)
            .pageLabel("Reasons for possession")
            .showCondition("claimDueToRentArrears=\"Yes\""
                               + " AND tenancy_TypeOfTenancyLicence=\"ASSURED_TENANCY\""
                               + " AND showRentArrearsGroundReasonPage=\"Yes\""
                               + " AND rentArrears_HasOtherAdditionalGrounds=\"Yes\""
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
                    Repossession by the landlord’s mortgage lender (ground 2)
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
            .done()
            .label("rentArrearsGroundsForPossessionReasons-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        RentArrearsGroundsReasons rentArrearsGrounds = caseData.getRentArrearsGroundsReasons();
        if (rentArrearsGrounds != null) {
            validationErrors.addAll(validateRentArrearsGrounds(rentArrearsGrounds));
        }

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private List<String> validateRentArrearsGrounds(RentArrearsGroundsReasons grounds) {
        List<TextAreaValidationService.FieldValidation> allValidations = new ArrayList<>();
        allValidations.addAll(List.of(buildMandatoryGroundValidations(grounds)));
        allValidations.addAll(List.of(buildDiscretionaryGroundValidations(grounds)));

        return textAreaValidationService.validateMultipleTextAreas(
            allValidations.toArray(new TextAreaValidationService.FieldValidation[0])
        );
    }

    private TextAreaValidationService.FieldValidation[] buildMandatoryGroundValidations(
            RentArrearsGroundsReasons grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getOwnerOccupierReason(),
                RentArrearsMandatoryGrounds.OWNER_OCCUPIER_GROUND1.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getRepossessionByLenderReason(),
                RentArrearsMandatoryGrounds.REPOSSESSION_GROUND2.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getHolidayLetReason(),
                RentArrearsMandatoryGrounds.HOLIDAY_LET_GROUND3.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getStudentLetReason(),
                RentArrearsMandatoryGrounds.STUDENT_LET_GROUND4.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getMinisterOfReligionReason(),
                RentArrearsMandatoryGrounds.MINISTER_RELIGION_GROUND5.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getRedevelopmentReason(),
                RentArrearsMandatoryGrounds.REDEVELOPMENT_GROUND6.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getDeathOfTenantReason(),
                RentArrearsMandatoryGrounds.DEATH_OF_TENANT_GROUND7.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getAntisocialBehaviourReason(),
                RentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR_GROUND7A.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getNoRightToRentReason(),
                RentArrearsMandatoryGrounds.NO_RIGHT_TO_RENT_GROUND7B.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }

    private TextAreaValidationService.FieldValidation[] buildDiscretionaryGroundValidations(
            RentArrearsGroundsReasons grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getSuitableAltAccommodationReason(),
                RentArrearsDiscretionaryGrounds.ALTERNATIVE_ACCOMMODATION_GROUND9.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getBreachOfTenancyConditionsReason(),
                RentArrearsDiscretionaryGrounds.BREACH_TENANCY_GROUND12.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getPropertyDeteriorationReason(),
                RentArrearsDiscretionaryGrounds.DETERIORATION_PROPERTY_GROUND13.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getNuisanceAnnoyanceReason(),
                RentArrearsDiscretionaryGrounds.NUISANCE_ANNOYANCE_GROUND14.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getDomesticViolenceReason(),
                RentArrearsDiscretionaryGrounds.DOMESTIC_VIOLENCE_GROUND14A.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getOffenceDuringRiotReason(),
                RentArrearsDiscretionaryGrounds.OFFENCE_RIOT_GROUND14ZA.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getFurnitureDeteriorationReason(),
                RentArrearsDiscretionaryGrounds.DETERIORATION_FURNITURE_GROUND15.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getEmployeeOfLandlordReason(),
                RentArrearsDiscretionaryGrounds.EMPLOYEE_LANDLORD_GROUND16.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getTenancyByFalseStatementReason(),
                RentArrearsDiscretionaryGrounds.FALSE_STATEMENT_GROUND17.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }
}
