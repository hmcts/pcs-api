package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ShowCondition;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.contains;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class RentArrearsGroundsForPossessionReasons implements CcdPageConfiguration {

    private static final ShowCondition.NamedFieldCondition ADDITIONAL_MANDATORY_GROUNDS = when(
        PCSCase::getAssuredRentArrearsPossessionGrounds,
        AssuredRentArrearsPossessionGrounds::getAdditionalMandatoryGrounds
    );
    private static final ShowCondition.NamedFieldCondition ADDITIONAL_DISCRETIONARY_GROUNDS = when(
        PCSCase::getAssuredRentArrearsPossessionGrounds,
        AssuredRentArrearsPossessionGrounds::getAdditionalDiscretionaryGrounds
    );

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("rentArrearsGroundsForPossessionReasons", this::midEvent)
            .pageLabel("Reasons for possession")
            .showWhen(when(PCSCase::getClaimDueToRentArrears).is(YesOrNo.YES)
                .and(when(PCSCase::getTenancyLicenceDetails, TenancyLicenceDetails::getTypeOfTenancyLicence)
                    .is(TenancyLicenceType.ASSURED_TENANCY))
                .and(when(PCSCase::getHasOtherAdditionalGrounds).is(YesOrNo.YES))
                .and(when(PCSCase::getLegislativeCountry).is(LegislativeCountry.ENGLAND)))
            .labelWhen("rentArrearsGrounds-lineSeparator","---")
            .complex(PCSCase::getRentArrearsGroundsReasons)

            // ---------- Mandatory grounds ----------
            .labelWhen("rentArrears-ownerOccupier-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Owner occupier (ground 1)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getOwnerOccupierReason,
                       contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1
                ))

            .labelWhen("rentArrears-repossessionByLender-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Repossession by the landlord’s mortgage lender (ground 2)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.REPOSSESSION_GROUND2
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getRepossessionByLenderReason,
                contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.REPOSSESSION_GROUND2
                ))

            .labelWhen("rentArrears-holidayLet-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Holiday let (ground 3)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.HOLIDAY_LET_GROUND3
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getHolidayLetReason,
                contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.HOLIDAY_LET_GROUND3
                ))

            .labelWhen("rentArrears-studentLet-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Student let (ground 4)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.STUDENT_LET_GROUND4
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getStudentLetReason,
                contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.STUDENT_LET_GROUND4
                ))

            .labelWhen("rentArrears-ministerOfReligion-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Property required for minister of religion (ground 5)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.MINISTER_RELIGION_GROUND5
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getMinisterOfReligionReason,
                contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.MINISTER_RELIGION_GROUND5
                ))

            .labelWhen("rentArrears-redevelopment-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Property required for redevelopment (ground 6)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.REDEVELOPMENT_GROUND6
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getRedevelopmentReason,
                contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.REDEVELOPMENT_GROUND6
                ))

            .labelWhen("rentArrears-deathOfTenant-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Death of the tenant (ground 7)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getDeathOfTenantReason,
                contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7
                ))

            .labelWhen("rentArrears-antisocialBehaviour-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Antisocial behaviour (ground 7A)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """,
                   contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getAntisocialBehaviourReason,
                contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A
                ))

            .labelWhen("rentArrears-noRightToRent-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Tenant does not have a right to rent (ground 7B)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getNoRightToRentReason,
                contains(
                    ADDITIONAL_MANDATORY_GROUNDS,
                    AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B
                ))

            // ---------- Discretionary grounds ----------
            .labelWhen("rentArrears-suitableAlternativeAccommodation-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Suitable alternative accommodation (ground 9)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getSuitableAltAccommodationReason,
                contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9
                ))

            .labelWhen("rentArrears-breachOfTenancyConditions-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Breach of tenancy conditions (ground 12)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getBreachOfTenancyConditionsReason,
                contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12
                ))

            .labelWhen("rentArrears-propertyDeterioration-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Deterioration in the condition of the property (ground 13)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getPropertyDeteriorationReason,
                contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13
                ))

            .labelWhen("rentArrears-nuisanceAnnoyance-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Nuisance, annoyance, illegal or immoral use of the property (ground 14)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getNuisanceAnnoyanceReason,
                contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14
                ))

            .labelWhen("rentArrears-domesticViolence-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Domestic violence (ground 14A)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getDomesticViolenceReason,
                contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A
                ))

            .labelWhen("rentArrears-offenceDuringRiot-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Offence during a riot (ground 14ZA)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getOffenceDuringRiotReason,
                contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA
                ))

            .labelWhen("rentArrears-furnitureDeterioration-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Deterioration of furniture (ground 15)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getFurnitureDeteriorationReason,
                contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15
                ))

            .labelWhen("rentArrears-employeeOfLandlord-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Employee of the landlord (ground 16)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getEmployeeOfLandlordReason,
                contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16
                ))

            .labelWhen("rentArrears-tenancyByFalseStatement-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Tenancy obtained by false statement (ground 17)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17
                ))
            .mandatoryWhen(RentArrearsGroundsReasons::getTenancyByFalseStatementReason,
                contains(
                    ADDITIONAL_DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17
                ))
            .done()
            .labelWhen("rentArrearsGroundsForPossessionReasons-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
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
                AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getRepossessionByLenderReason(),
                AssuredMandatoryGround.REPOSSESSION_GROUND2.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getHolidayLetReason(),
                AssuredMandatoryGround.HOLIDAY_LET_GROUND3.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getStudentLetReason(),
                AssuredMandatoryGround.STUDENT_LET_GROUND4.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getMinisterOfReligionReason(),
                AssuredMandatoryGround.MINISTER_RELIGION_GROUND5.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getRedevelopmentReason(),
                AssuredMandatoryGround.REDEVELOPMENT_GROUND6.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getDeathOfTenantReason(),
                AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getAntisocialBehaviourReason(),
                AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getNoRightToRentReason(),
                AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }

    private TextAreaValidationService.FieldValidation[] buildDiscretionaryGroundValidations(
            RentArrearsGroundsReasons grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getSuitableAltAccommodationReason(),
                AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getBreachOfTenancyConditionsReason(),
                AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getPropertyDeteriorationReason(),
                AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getNuisanceAnnoyanceReason(),
                AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getDomesticViolenceReason(),
                AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getOffenceDuringRiotReason(),
                AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getFurnitureDeteriorationReason(),
                AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getEmployeeOfLandlordReason(),
                AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getTenancyByFalseStatementReason(),
                AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }
}
