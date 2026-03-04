package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ShowCondition;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.contains;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class NoRentArrearsGroundsForPossessionReason implements CcdPageConfiguration {

    private static final ShowCondition.NamedFieldCondition MANDATORY_GROUNDS = when(
        PCSCase::getNoRentArrearsGroundsOptions,
        AssuredNoArrearsPossessionGrounds::getMandatoryGrounds
    );
    private static final ShowCondition.NamedFieldCondition DISCRETIONARY_GROUNDS = when(
        PCSCase::getNoRentArrearsGroundsOptions,
        AssuredNoArrearsPossessionGrounds::getDiscretionaryGrounds
    );

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("noRentArrearsGroundsForPossessionReason", this::midEvent)
            .pageLabel("Reasons for possession")
            .showWhen(when(PCSCase::getClaimDueToRentArrears).is(YesOrNo.NO)
                .and(when(PCSCase::getTenancyLicenceDetails, TenancyLicenceDetails::getTypeOfTenancyLicence)
                    .is(TenancyLicenceType.ASSURED_TENANCY))
                .and(when(PCSCase::getNoRentArrearsGroundsOptions,
                    AssuredNoArrearsPossessionGrounds::getShowGroundReasonPage).is(YesOrNo.YES))
                .and(when(PCSCase::getLegislativeCountry).is(LegislativeCountry.ENGLAND)))
            .labelWhen("noRentArrearsOptions-lineSeparator", "---")
            .complex(PCSCase::getNoRentArrearsReasonForGrounds)
            // Ground 1
            .labelWhen(
                "ownerOccupier-label",
                """
                    <h2 class="govuk-heading-l">Owner occupier (ground 1)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getOwnerOccupier,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1
                )
            )
            // Ground 2
            .labelWhen(
                "repossessionByLender-label",
                """
                    <h2 class="govuk-heading-l">Repossession by the landlord’s mortgage lender (ground 2)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.REPOSSESSION_GROUND2
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getRepossessionByLender,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.REPOSSESSION_GROUND2
                )
            )
            // Ground 3
            .labelWhen(
                "holidayLet-label",
                """
                    <h2 class="govuk-heading-l">Holiday let (ground 3)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.HOLIDAY_LET_GROUND3
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getHolidayLet,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.HOLIDAY_LET_GROUND3
                )
            )
            // Ground 4
            .labelWhen(
                "studentLet-label",
                """
                    <h2 class="govuk-heading-l">Student let (ground 4)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.STUDENT_LET_GROUND4
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getStudentLet,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.STUDENT_LET_GROUND4
                )
            )
            // Ground 5
            .labelWhen(
                "ministerOfReligion-label",
                """
                    <h2 class="govuk-heading-l">Property required for minister of religion (ground 5)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.MINISTER_RELIGION_GROUND5
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getMinisterOfReligion,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.MINISTER_RELIGION_GROUND5
                )
            )
            // Ground 6
            .labelWhen(
                "redevelopment-label",
                """
                    <h2 class="govuk-heading-l">Property required for redevelopment (ground 6)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.REDEVELOPMENT_GROUND6
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getRedevelopment,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.REDEVELOPMENT_GROUND6
                )
            )
            // Ground 7
            .labelWhen(
                "deathOfTenant-label",
                """
                    <h2 class="govuk-heading-l">Death of the tenant (ground 7)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getDeathOfTenant,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7
                )
            )
            // Ground 7A
            .labelWhen(
                "antisocialBehaviour-label",
                """
                    <h2 class="govuk-heading-l">Antisocial behaviour (ground 7A)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getAntisocialBehaviour,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A
                )
            )
            // Ground 7B
            .labelWhen(
                "noRightToRent-label",
                """
                    <h2 class="govuk-heading-l">Tenant does not have a right to rent (ground 7B)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getNoRightToRent,
                contains(
                    MANDATORY_GROUNDS,
                    AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B
                )
            )
            // Ground 9
            .labelWhen(
                "suitableAccom-label",
                """
                    <h2 class="govuk-heading-l">Suitable alternative accommodation (ground 9)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getSuitableAlternativeAccomodation,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9
                )
            )
            // Ground 12
            .labelWhen(
                "breachOfTenancyConditions-label",
                """
                    <h2 class="govuk-heading-l">Breach of tenancy conditions (ground 12)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getBreachOfTenancyConditions,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12
                )
            )
            // Ground 13
            .labelWhen(
                "propertyDeterioration-label",
                """
                    <h2 class="govuk-heading-l">Deterioration in the condition of the property (ground 13)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getPropertyDeterioration,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13
                )
            )
            // Ground 14
            .labelWhen(
                "nuisanceOrIllegalUse-label",
                """
                    <h2 class="govuk-heading-l">Nuisance, annoyance, illegal or immoral use of the property
                    (ground 14)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getNuisanceOrIllegalUse,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14
                )
            )
            // Ground 14A
            .labelWhen(
                "domesticViolence-label",
                """
                    <h2 class="govuk-heading-l">Domestic violence (ground 14A)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getDomesticViolence,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A
                )
            )
            // Ground 14ZA
            .labelWhen(
                "offenceDuringRiot-label",
                """
                    <h2 class="govuk-heading-l">Offence during a riot (ground 14ZA)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getOffenceDuringRiot,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA
                )
            )
            // Ground 15
            .labelWhen(
                "furnitureDeterioration-label",
                """
                    <h2 class="govuk-heading-l">Deterioration of furniture (ground 15)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getFurnitureDeterioration,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15
                )
            )
            // Ground 16
            .labelWhen(
                "landlordEmployee-label",
                """
                    <h2 class="govuk-heading-l">Employee of the landlord (ground 16)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getLandlordEmployee,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16
                )
            )
            // Ground 17
            .labelWhen(
                "falseStatement-label",
                """
                    <h2 class="govuk-heading-l">Tenancy obtained by false statement (ground 17)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17
                )
            )
            .mandatoryWhen(
                NoRentArrearsReasonForGrounds::getFalseStatement,
                contains(
                    DISCRETIONARY_GROUNDS,
                    AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17
                )
            )
            .done()
            .labelWhen("noRentArrearsGroundsForPossessionReason-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
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
                    noRentArrearsReasonForGrounds.getOwnerOccupier(),
                    AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getRepossessionByLender(),
                    AssuredMandatoryGround.REPOSSESSION_GROUND2.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getHolidayLet(),
                    AssuredMandatoryGround.HOLIDAY_LET_GROUND3.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getStudentLet(),
                    AssuredMandatoryGround.STUDENT_LET_GROUND4.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getMinisterOfReligion(),
                    AssuredMandatoryGround.MINISTER_RELIGION_GROUND5.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getRedevelopment(),
                    AssuredMandatoryGround.REDEVELOPMENT_GROUND6.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getDeathOfTenant(),
                    AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getAntisocialBehaviour(),
                    AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getNoRightToRent(),
                    AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                // Discretionary grounds
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getSuitableAlternativeAccomodation(),
                    AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getBreachOfTenancyConditions(),
                    AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getPropertyDeterioration(),
                    AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getNuisanceOrIllegalUse(),
                    AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getDomesticViolence(),
                    AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getOffenceDuringRiot(),
                    AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getFurnitureDeterioration(),
                    AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getLandlordEmployee(),
                    AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    noRentArrearsReasonForGrounds.getFalseStatement(),
                    AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                )
            ));
        }

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
