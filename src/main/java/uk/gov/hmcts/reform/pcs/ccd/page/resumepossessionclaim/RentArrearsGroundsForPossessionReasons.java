package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
                               + " AND typeOfTenancyLicence=\"ASSURED_TENANCY\""
                               + " AND showRentArrearsGroundReasonPage=\"Yes\""
                               + " AND hasOtherAdditionalGrounds=\"Yes\""
                               + " AND legislativeCountry=\"England\""
            )
            .label("rentArrearsGrounds-lineSeparator","---")
            //.complex(PCSCase::getRentArrearsGroundsReasons)
            .complex(PCSCase::getRentArrearsAdditionalGrounds)
            .readonly(RentArrearsAdditionalGrounds::getMandatoryGrounds)
            .readonly(RentArrearsAdditionalGrounds::getDiscretionaryGrounds)

            .optional(RentArrearsAdditionalGrounds::getAssuredAdditionalMandatoryGrounds)
            .optional(RentArrearsAdditionalGrounds::getAssuredAdditionalDiscretionaryGrounds)
            .done()
            .complex(PCSCase::getRentArrearsGroundsReasons)
            .readonly(RentArrearsGroundsReasons::getShowOwnerOccupierReason)
            .readonly(RentArrearsGroundsReasons::getShowRepossessionByLenderReason)
            .readonly(RentArrearsGroundsReasons::getShowHolidayLetReason)
            .readonly(RentArrearsGroundsReasons::getShowStudentLetReason)
            .readonly(RentArrearsGroundsReasons::getShowMinisterOfReligionReason)
            .readonly(RentArrearsGroundsReasons::getShowRedevelopmentReason)
            .readonly(RentArrearsGroundsReasons::getShowDeathOfTenantReason)
            .readonly(RentArrearsGroundsReasons::getShowAntisocialBehaviourReason)
            .readonly(RentArrearsGroundsReasons::getShowNoRightToRentReason)

            .readonly(RentArrearsGroundsReasons::getShowSuitableAltAccommodationReason)
            .readonly(RentArrearsGroundsReasons::getShowBreachOfTenancyConditionsReason)
            .readonly(RentArrearsGroundsReasons::getShowPropertyDeteriorationReason)
            .readonly(RentArrearsGroundsReasons::getShowNuisanceAnnoyanceReason)
            .readonly(RentArrearsGroundsReasons::getShowDomesticViolenceReason)
            .readonly(RentArrearsGroundsReasons::getShowOffenceDuringRiotReason)
            .readonly(RentArrearsGroundsReasons::getShowFurnitureDeteriorationReason)
            .readonly(RentArrearsGroundsReasons::getShowEmployeeOfLandlordReason)
            .readonly(RentArrearsGroundsReasons::getShowTenancyByFalseStatementReason)

            .label("rentArrears-ownerOccupier-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Owner occupier (ground 1)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showOwnerOccupierReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getOwnerOccupierReason,
                       "showOwnerOccupierReason=\"Yes\"")

            .label("rentArrears-repossessionByLender-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Repossession by the landlord’s mortgage lender (ground 2)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showRepossessionByLenderReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getRepossessionByLenderReason,
                       "showRepossessionByLenderReason=\"Yes\"")

            .label("rentArrears-holidayLet-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Holiday let (ground 3)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showHolidayLetReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getHolidayLetReason,
                       "showHolidayLetReason=\"Yes\"")

            .label("rentArrears-studentLet-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Student let (ground 4)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showStudentLetReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getStudentLetReason,
                       "showStudentLetReason=\"Yes\"")

            .label("rentArrears-ministerOfReligion-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Property required for minister of religion (ground 5)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showMinisterOfReligionReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getMinisterOfReligionReason,
                       "showMinisterOfReligionReason=\"Yes\"")

            .label("rentArrears-redevelopment-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Property required for redevelopment (ground 6)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """,  "showRedevelopmentReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getRedevelopmentReason,
                       "showRedevelopmentReason=\"Yes\"")

            .label("rentArrears-deathOfTenant-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Death of the tenant (ground 7)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showDeathOfTenantReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getDeathOfTenantReason,
                       "showDeathOfTenantReason=\"Yes\"")

            .label("rentArrears-antisocialBehaviour-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Antisocial behaviour (ground 7A)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showAntisocialBehaviourReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getAntisocialBehaviourReason,
                       "showAntisocialBehaviourReason=\"Yes\"")

            .label("rentArrears-noRightToRent-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Tenant does not have a right to rent (ground 7B)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showNoRightToRentReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getNoRightToRentReason,
                       "showNoRightToRentReason=\"Yes\"")

            // ---------- Discretionary grounds ----------
            .label("rentArrears-suitableAlternativeAccommodation-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Suitable alternative accommodation (ground 9)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showSuitableAltAccommodationReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getSuitableAltAccommodationReason,
                       "showSuitableAltAccommodationReason=\"Yes\"")

            .label("rentArrears-breachOfTenancyConditions-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Breach of tenancy conditions (ground 12)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showBreachOfTenancyConditionsReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getBreachOfTenancyConditionsReason,
                       "showBreachOfTenancyConditionsReason=\"Yes\"")

            .label("rentArrears-propertyDeterioration-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Deterioration in the condition of the property (ground 13)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showPropertyDeteriorationReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getPropertyDeteriorationReason,
                       "showPropertyDeteriorationReason=\"Yes\"")

            .label("rentArrears-nuisanceAnnoyance-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Nuisance, annoyance, illegal or immoral use of the property (ground 14)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showNuisanceAnnoyanceReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getNuisanceAnnoyanceReason,
                       "showNuisanceAnnoyanceReason=\"Yes\"")

            .label("rentArrears-domesticViolence-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Domestic violence (ground 14A)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showDomesticViolenceReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getDomesticViolenceReason,
                       "showDomesticViolenceReason=\"Yes\"")

            .label("rentArrears-offenceDuringRiot-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Offence during a riot (ground 14ZA)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showOffenceDuringRiotReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getOffenceDuringRiotReason,
                       "showOffenceDuringRiotReason=\"Yes\"")

            .label("rentArrears-furnitureDeterioration-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Deterioration of furniture (ground 15)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showFurnitureDeteriorationReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getFurnitureDeteriorationReason,
                       "showFurnitureDeteriorationReason=\"Yes\"")

            .label("rentArrears-employeeOfLandlord-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Employee of the landlord (ground 16)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showEmployeeOfLandlordReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getEmployeeOfLandlordReason,
                       "showEmployeeOfLandlordReason=\"Yes\"")

            .label("rentArrears-tenancyByFalseStatement-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Tenancy obtained by false statement (ground 17)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "showTenancyByFalseStatementReason=\"Yes\"")
            .mandatory(RentArrearsGroundsReasons::getTenancyByFalseStatementReason,
                       "showTenancyByFalseStatementReason=\"Yes\"")
            .done()
            .label("rentArrearsGroundsForPossessionReasons-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        RentArrearsAdditionalGrounds additional =
            caseData.getRentArrearsAdditionalGrounds();
        RentArrearsGroundsReasons reasons =
            caseData.getRentArrearsGroundsReasons();

        if (additional != null && reasons != null) {

            Set<RentArrearsMandatoryGrounds> mandatory =
                additional.getMandatoryGrounds();

            Set<RentArrearsDiscretionaryGrounds> discretionary =
                additional.getDiscretionaryGrounds();


            reasons.setShowOwnerOccupierReason(
                YesOrNo.from(mandatory.contains(
                    RentArrearsMandatoryGrounds.OWNER_OCCUPIER_GROUND1)));
            reasons.setShowRepossessionByLenderReason(
                YesOrNo.from(mandatory.contains(
                    RentArrearsMandatoryGrounds.REPOSSESSION_GROUND2
                ))
            );

            reasons.setShowHolidayLetReason(
                YesOrNo.from(mandatory != null
                                 && mandatory.contains(RentArrearsMandatoryGrounds.HOLIDAY_LET_GROUND3)));

            reasons.setShowStudentLetReason(
                YesOrNo.from(mandatory != null
                                 && mandatory.contains(RentArrearsMandatoryGrounds.STUDENT_LET_GROUND4)));

            reasons.setShowMinisterOfReligionReason(
                YesOrNo.from(mandatory != null
                                 && mandatory.contains(RentArrearsMandatoryGrounds.MINISTER_RELIGION_GROUND5)));

            reasons.setShowRedevelopmentReason(
                YesOrNo.from(mandatory != null
                                 && mandatory.contains(RentArrearsMandatoryGrounds.REDEVELOPMENT_GROUND6)));

            reasons.setShowDeathOfTenantReason(
                YesOrNo.from(mandatory != null
                                 && mandatory.contains(RentArrearsMandatoryGrounds.DEATH_OF_TENANT_GROUND7)));

            reasons.setShowAntisocialBehaviourReason(
                YesOrNo.from(mandatory != null
                                 && mandatory.contains(RentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR_GROUND7A)));

            reasons.setShowNoRightToRentReason(
                YesOrNo.from(mandatory != null
                                 && mandatory.contains(RentArrearsMandatoryGrounds.NO_RIGHT_TO_RENT_GROUND7B)));

            // ---------- Discretionary grounds ----------
            reasons.setShowSuitableAltAccommodationReason(
                YesOrNo.from(discretionary != null
                                 && discretionary.contains(RentArrearsDiscretionaryGrounds
                                                               .ALTERNATIVE_ACCOMMODATION_GROUND9)));

            reasons.setShowBreachOfTenancyConditionsReason(
                YesOrNo.from(discretionary != null
                                 && discretionary.contains(RentArrearsDiscretionaryGrounds.BREACH_TENANCY_GROUND12)));

            reasons.setShowPropertyDeteriorationReason(
                YesOrNo.from(discretionary != null
                                 && discretionary.contains(RentArrearsDiscretionaryGrounds
                                                               .DETERIORATION_PROPERTY_GROUND13)));

            reasons.setShowNuisanceAnnoyanceReason(
                YesOrNo.from(discretionary != null
                                 && discretionary.contains(RentArrearsDiscretionaryGrounds
                                                               .NUISANCE_ANNOYANCE_GROUND14)));

            reasons.setShowDomesticViolenceReason(
                YesOrNo.from(discretionary != null
                                 && discretionary.contains(RentArrearsDiscretionaryGrounds
                                                               .DOMESTIC_VIOLENCE_GROUND14A)));

            reasons.setShowOffenceDuringRiotReason(
                YesOrNo.from(discretionary != null
                                 && discretionary.contains(RentArrearsDiscretionaryGrounds.OFFENCE_RIOT_GROUND14ZA)));

            reasons.setShowFurnitureDeteriorationReason(
                YesOrNo.from(discretionary != null
                                 && discretionary.contains(RentArrearsDiscretionaryGrounds
                                                               .DETERIORATION_FURNITURE_GROUND15)));

            reasons.setShowEmployeeOfLandlordReason(
                YesOrNo.from(discretionary != null
                                 && discretionary.contains(RentArrearsDiscretionaryGrounds
                                                               .EMPLOYEE_LANDLORD_GROUND16)));

            reasons.setShowTenancyByFalseStatementReason(
                YesOrNo.from(discretionary != null
                                 && discretionary.contains(RentArrearsDiscretionaryGrounds.FALSE_STATEMENT_GROUND17)));

        }

        if (reasons != null) {
            validationErrors.addAll(validateRentArrearsGrounds(reasons));
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
