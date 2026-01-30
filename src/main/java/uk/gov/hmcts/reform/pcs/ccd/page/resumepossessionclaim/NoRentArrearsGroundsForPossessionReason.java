package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class NoRentArrearsGroundsForPossessionReason implements CcdPageConfiguration {

    private static final String MANDATORY_GROUNDS = "noRentArrears_MandatoryGrounds";
    private static final String DISCRETIONARY_GROUNDS = "noRentArrears_DiscretionaryGrounds";

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("noRentArrearsGroundsForPossessionReason", this::midEvent)
            .pageLabel("Reasons for possession")
            .showCondition("claimDueToRentArrears=\"No\" "
                               + "AND tenancy_TypeOfTenancyLicence=\"ASSURED_TENANCY\""
                               + " AND noRentArrears_ShowGroundReasonPage=\"Yes\""
                               + " AND legislativeCountry=\"England\"")
            .label("noRentArrearsOptions-lineSeparator", "---")
            .complex(PCSCase::getNoRentArrearsReasonForGrounds)
            // Ground 1
            .label(
                "noRentArrearsOptions-ownerOccupier-label",
                """
                    <h2 class="govuk-heading-l">Owner occupier (ground 1)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getOwnerOccupierTextArea,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1)
            )
            // Ground 2
            .label(
                "noRentArrearsOptions-repossessionByLender-label",
                """
                    <h2 class="govuk-heading-l">Repossession by the landlord’s mortgage lender (ground 2)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.REPOSSESSION_GROUND2)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getRepossessionByLenderTextArea,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.REPOSSESSION_GROUND2)
            )
            // Ground 3
            .label(
                "noRentArrearsOptions-holidayLet-label",
                """
                    <h2 class="govuk-heading-l">Holiday let (ground 3)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.HOLIDAY_LET_GROUND3)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getHolidayLetTextArea,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.HOLIDAY_LET_GROUND3)
            )
            // Ground 4
            .label(
                "noRentArrearsOptions-studentLet-label",
                """
                    <h2 class="govuk-heading-l">Student let (ground 4)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.STUDENT_LET_GROUND4)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getStudentLetTextArea,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.STUDENT_LET_GROUND4)
            )
            // Ground 5
            .label(
                "noRentArrearsOptions-ministerOfReligion-label",
                """
                    <h2 class="govuk-heading-l">Property required for minister of religion (ground 5)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.MINISTER_RELIGION_GROUND5)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getMinisterOfReligionTextArea,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.MINISTER_RELIGION_GROUND5)
            )
            // Ground 6
            .label(
                "noRentArrearsOptions-redevelopment-label",
                """
                    <h2 class="govuk-heading-l">Property required for redevelopment (ground 6)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.REDEVELOPMENT_GROUND6)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getRedevelopmentTextArea,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.REDEVELOPMENT_GROUND6)
            )
            // Ground 7
            .label(
                "noRentArrearsOptions-deathOfTenant-label",
                """
                    <h2 class="govuk-heading-l">Death of the tenant (ground 7)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getDeathOfTenantTextArea,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7)
            )
            // Ground 7A
            .label(
                "noRentArrearsOptions-antisocialBehaviour-label",
                """
                    <h2 class="govuk-heading-l">Antisocial behaviour (ground 7A)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getAntisocialBehaviourTextArea,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A)
            )
            // Ground 7B
            .label(
                "noRentArrearsOptions-noRightToRent-label",
                """
                    <h2 class="govuk-heading-l">Tenant does not have a right to rent (ground 7B)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getNoRightToRentTextArea,
                ShowConditions.fieldContains(MANDATORY_GROUNDS, AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B)
            )
            // Ground 9
            .label(
                "noRentArrearsOptions-suitableAccom-label",
                """
                    <h2 class="govuk-heading-l">Suitable alternative accommodation (ground 9)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getSuitableAccomTextArea,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9)
            )
            // Ground 12
            .label(
                "noRentArrearsOptions-breachOfTenancyConditions-label",
                """
                    <h2 class="govuk-heading-l">Breach of tenancy conditions (ground 12)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getBreachOfTenancyConditionsTextArea,
                ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12)
            )
            // Ground 13
            .label(
                "noRentArrearsOptions-propertyDeterioration-label",
                """
                    <h2 class="govuk-heading-l">Deterioration in the condition of the property (ground 13)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getPropertyDeteriorationTextArea,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13)
            )
            // Ground 14
            .label(
                "noRentArrearsOptions-nuisanceOrIllegalUse-label",
                """
                    <h2 class="govuk-heading-l">Nuisance, annoyance, illegal or immoral use of the property
                    (ground 14)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getNuisanceOrIllegalUseTextArea,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14)
            )
            // Ground 14A
            .label(
                "noRentArrearsOptions-domesticViolence-label",
                """
                    <h2 class="govuk-heading-l">Domestic violence (ground 14A)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getDomesticViolenceTextArea,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A)
            )
            // Ground 14ZA
            .label(
                "noRentArrearsOptions-offenceDuringRiot-label",
                """
                    <h2 class="govuk-heading-l">Offence during a riot (ground 14ZA)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getOffenceDuringRiotTextArea,
                ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA)
            )
            // Ground 15
            .label(
                "noRentArrearsOptions-furnitureDeterioration-label",
                """
                    <h2 class="govuk-heading-l">Deterioration of furniture (ground 15)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getFurnitureDeteriorationTextArea,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15)
            )
            // Ground 16
            .label(
                "noRentArrearsOptions-landlordEmployee-label",
                """
                    <h2 class="govuk-heading-l">Employee of the landlord (ground 16)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getLandlordEmployeeTextArea,
                ShowConditions
                    .fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16)
            )
            // Ground 17
            .label(
                "noRentArrearsOptions-falseStatement-label",
                """
                    <h2 class="govuk-heading-l">Tenancy obtained by false statement (ground 17)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17)
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getFalseStatementTextArea,
                ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17)
            )
            .done()
            .label("noRentArrearsGroundsForPossessionReason-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        // Validate all text area fields for character limit
        List<String> validationErrors = new ArrayList<>();

        NoRentArrearsReasonForGrounds noRentArrearsReasonForGrounds = caseData.getNoRentArrearsReasonForGrounds();
        if (noRentArrearsReasonForGrounds != null) {
            validationErrors.addAll(textAreaValidationService.validateMultipleTextAreas(
                // Mandatory grounds
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getOwnerOccupierTextArea(),
                    AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getRepossessionByLenderTextArea(),
                    AssuredMandatoryGround.REPOSSESSION_GROUND2.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getHolidayLetTextArea(),
                    AssuredMandatoryGround.HOLIDAY_LET_GROUND3.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getStudentLetTextArea(),
                    AssuredMandatoryGround.STUDENT_LET_GROUND4.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getMinisterOfReligionTextArea(),
                    AssuredMandatoryGround.MINISTER_RELIGION_GROUND5.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getRedevelopmentTextArea(),
                    AssuredMandatoryGround.REDEVELOPMENT_GROUND6.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getDeathOfTenantTextArea(),
                    AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getAntisocialBehaviourTextArea(),
                    AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getNoRightToRentTextArea(),
                    AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                // Discretionary grounds
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getSuitableAccomTextArea(),
                    AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getBreachOfTenancyConditionsTextArea(),
                    AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getPropertyDeteriorationTextArea(),
                    AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getNuisanceOrIllegalUseTextArea(),
                    AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getDomesticViolenceTextArea(),
                    AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getOffenceDuringRiotTextArea(),
                    AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getFurnitureDeteriorationTextArea(),
                    AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getLandlordEmployeeTextArea(),
                    AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getFalseStatementTextArea(),
                    AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                )
            ));
        }

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
