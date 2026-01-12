package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class NoRentArrearsGroundsForPossessionReason implements CcdPageConfiguration {

    public static final String NO_RENT_ARREARS_OPTIONS_MANDATORY = "noRentArrears_MandatoryGrounds";
    public static final String NO_RENT_ARREARS_OPTIONS_DISCRETIONARY = "noRentArrears_DiscretionaryGrounds";
    private final TextValidationService textValidationService;

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
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"OWNER_OCCUPIER\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getOwnerOccupierTextArea,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"OWNER_OCCUPIER\""
            )
            // Ground 2
            .label(
                "noRentArrearsOptions-repossessionByLender-label",
                """
                    <h2 class="govuk-heading-l">Repossession by the landlord’s mortgage lender (ground 2)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"REPOSSESSION_BY_LENDER\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getRepossessionByLenderTextArea,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"REPOSSESSION_BY_LENDER\""
            )
            // Ground 3
            .label(
                "noRentArrearsOptions-holidayLet-label",
                """
                    <h2 class="govuk-heading-l">Holiday let (ground 3)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"HOLIDAY_LET\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getHolidayLetTextArea,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"HOLIDAY_LET\""
            )
            // Ground 4
            .label(
                "noRentArrearsOptions-studentLet-label",
                """
                    <h2 class="govuk-heading-l">Student let (ground 4)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """, NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"STUDENT_LET\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getStudentLetTextArea,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"STUDENT_LET\""
            )
            // Ground 5
            .label(
                "noRentArrearsOptions-ministerOfReligion-label",
                """
                    <h2 class="govuk-heading-l">Property required for minister of religion (ground 5)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"MINISTER_OF_RELIGION\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getMinisterOfReligionTextArea,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"MINISTER_OF_RELIGION\""
            )
            // Ground 6
            .label(
                "noRentArrearsOptions-redevelopment-label",
                """
                    <h2 class="govuk-heading-l">Property required for redevelopment (ground 6)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"REDEVELOPMENT\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getRedevelopmentTextArea,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"REDEVELOPMENT\""
            )
            // Ground 7
            .label(
                "noRentArrearsOptions-deathOfTenant-label",
                """
                    <h2 class="govuk-heading-l">Death of the tenant (ground 7)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"DEATH_OF_TENANT\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getDeathOfTenantTextArea,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"DEATH_OF_TENANT\""
            )
            // Ground 7A
            .label(
                "noRentArrearsOptions-antisocialBehaviour-label",
                """
                    <h2 class="govuk-heading-l">Antisocial behaviour (ground 7A)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"ANTISOCIAL_BEHAVIOUR\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getAntisocialBehaviourTextArea,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"ANTISOCIAL_BEHAVIOUR\""
            )
            // Ground 7B
            .label(
                "noRentArrearsOptions-noRightToRent-label",
                """
                    <h2 class="govuk-heading-l">Tenant does not have a right to rent (ground 7B)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"NO_RIGHT_TO_RENT\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getNoRightToRentTextArea,
                NO_RENT_ARREARS_OPTIONS_MANDATORY + "CONTAINS\"NO_RIGHT_TO_RENT\""
            )
            // Ground 9
            .label(
                "noRentArrearsOptions-suitableAccom-label",
                """
                    <h2 class="govuk-heading-l">Suitable alternative accommodation (ground 9)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"SUITABLE_ACCOM\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getSuitableAccomTextArea,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"SUITABLE_ACCOM\""
            )
            // Ground 12
            .label(
                "noRentArrearsOptions-breachOfTenancyConditions-label",
                """
                    <h2 class="govuk-heading-l">Breach of tenancy conditions (ground 12)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"BREACH_OF_TENANCY_CONDITIONS\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getBreachOfTenancyConditionsTextArea,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"BREACH_OF_TENANCY_CONDITIONS\""
            )
            // Ground 13
            .label(
                "noRentArrearsOptions-propertyDeterioration-label",
                """
                    <h2 class="govuk-heading-l">Deterioration in the condition of the property (ground 13)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"PROPERTY_DETERIORATION\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getPropertyDeteriorationTextArea,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"PROPERTY_DETERIORATION\""
            )
            // Ground 14
            .label(
                "noRentArrearsOptions-nuisanceOrIllegalUse-label",
                """
                    <h2 class="govuk-heading-l">Nuisance, annoyance, illegal or immoral use of the property
                    (ground 14)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"NUISANCE_OR_ILLEGAL_USE\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getNuisanceOrIllegalUseTextArea,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"NUISANCE_OR_ILLEGAL_USE\""
            )
            // Ground 14A
            .label(
                "noRentArrearsOptions-domesticViolence-label",
                """
                    <h2 class="govuk-heading-l">Domestic violence (ground 14A)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"DOMESTIC_VIOLENCE\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getDomesticViolenceTextArea,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"DOMESTIC_VIOLENCE\""
            )
            // Ground 14ZA
            .label(
                "noRentArrearsOptions-offenceDuringRiot-label",
                """
                    <h2 class="govuk-heading-l">Offence during a riot (ground 14ZA)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"OFFENCE_DURING_RIOT\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getOffenceDuringRiotTextArea,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"OFFENCE_DURING_RIOT\""
            )
            // Ground 15
            .label(
                "noRentArrearsOptions-furnitureDeterioration-label",
                """
                    <h2 class="govuk-heading-l">Deterioration of furniture (ground 15)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"FURNITURE_DETERIORATION\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getFurnitureDeteriorationTextArea,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"FURNITURE_DETERIORATION\""
            )
            // Ground 16
            .label(
                "noRentArrearsOptions-landlordEmployee-label",
                """
                    <h2 class="govuk-heading-l">Employee of the landlord (ground 16)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"LANDLORD_EMPLOYEE\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getLandlordEmployeeTextArea,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"LANDLORD_EMPLOYEE\""
            )
            // Ground 17
            .label(
                "noRentArrearsOptions-falseStatement-label",
                """
                    <h2 class="govuk-heading-l">Tenancy obtained by false statement (ground 17)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"FALSE_STATEMENT\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getFalseStatementTextArea,
                NO_RENT_ARREARS_OPTIONS_DISCRETIONARY + "CONTAINS\"FALSE_STATEMENT\""
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
            validationErrors.addAll(textValidationService.validateMultipleTextAreas(
                // Mandatory grounds
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getOwnerOccupierTextArea(),
                    NoRentArrearsMandatoryGrounds.OWNER_OCCUPIER.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getRepossessionByLenderTextArea(),
                    NoRentArrearsMandatoryGrounds.REPOSSESSION_BY_LENDER.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getHolidayLetTextArea(),
                    NoRentArrearsMandatoryGrounds.HOLIDAY_LET.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getStudentLetTextArea(),
                    NoRentArrearsMandatoryGrounds.STUDENT_LET.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getMinisterOfReligionTextArea(),
                    NoRentArrearsMandatoryGrounds.MINISTER_OF_RELIGION.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getRedevelopmentTextArea(),
                    NoRentArrearsMandatoryGrounds.REDEVELOPMENT.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getDeathOfTenantTextArea(),
                    NoRentArrearsMandatoryGrounds.DEATH_OF_TENANT.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getAntisocialBehaviourTextArea(),
                    NoRentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getNoRightToRentTextArea(),
                    NoRentArrearsMandatoryGrounds.NO_RIGHT_TO_RENT.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                // Discretionary grounds
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getSuitableAccomTextArea(),
                    NoRentArrearsDiscretionaryGrounds.SUITABLE_ACCOM.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getBreachOfTenancyConditionsTextArea(),
                    NoRentArrearsDiscretionaryGrounds.BREACH_OF_TENANCY_CONDITIONS.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getPropertyDeteriorationTextArea(),
                    NoRentArrearsDiscretionaryGrounds.PROPERTY_DETERIORATION.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getNuisanceOrIllegalUseTextArea(),
                    NoRentArrearsDiscretionaryGrounds.NUISANCE_OR_ILLEGAL_USE.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getDomesticViolenceTextArea(),
                    NoRentArrearsDiscretionaryGrounds.DOMESTIC_VIOLENCE.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getOffenceDuringRiotTextArea(),
                    NoRentArrearsDiscretionaryGrounds.OFFENCE_DURING_RIOT.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getFurnitureDeteriorationTextArea(),
                    NoRentArrearsDiscretionaryGrounds.FURNITURE_DETERIORATION.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getLandlordEmployeeTextArea(),
                    NoRentArrearsDiscretionaryGrounds.LANDLORD_EMPLOYEE.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getFalseStatementTextArea(),
                    NoRentArrearsDiscretionaryGrounds.FALSE_STATEMENT.getLabel(),
                    TextValidationService.MEDIUM_TEXT_LIMIT
                )
            ));
        }

        return textValidationService.createValidationResponse(caseData, validationErrors);
    }
}
