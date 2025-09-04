package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.ReasonForGrounds;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Placeholder page configuration for the Grounds for Possession section.
 * Full implementation will be done in another ticket - responses not captured at the moment.
 */
@AllArgsConstructor
@Component
@Slf4j
public class GroundsForPossessionReason implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundsForPossessionReason", this::midEvent)
            .pageLabel("Reason for possession")
            .showCondition("groundsForPossession=\"No\"")
            .label("groundsForPossessionReason-lineSeparator", "---")
            .readonly(PCSCase::getSelectedMandatoryGrounds, NEVER_SHOW)
            .readonly(PCSCase::getSelectedDiscretionaryGrounds, NEVER_SHOW)
            .complex(PCSCase::getReasonForGrounds)
            // Ground 1
            .readonly(
                ReasonForGrounds::getOwnerOccupierLabel,
                "selectedMandatoryGroundsCONTAINS\"OWNER_OCCUPIER\""
            )
            .readonly(
                ReasonForGrounds::getOwnerOccupierGroundsLabel,
                "selectedMandatoryGroundsCONTAINS\"OWNER_OCCUPIER\""
            )
            .mandatory(
                ReasonForGrounds::getOwnerOccupierTextArea,
                "selectedMandatoryGroundsCONTAINS\"OWNER_OCCUPIER\""
            )
            // Ground 2
            .readonly(
                ReasonForGrounds::getRepossessionByLenderLabel,
                "selectedMandatoryGroundsCONTAINS\"REPOSSESSION_BY_LENDER\""
            )
            .readonly(
                ReasonForGrounds::getRepossessionByLenderGroundsLabel,
                "selectedMandatoryGroundsCONTAINS\"REPOSSESSION_BY_LENDER\""
            )
            .mandatory(
                ReasonForGrounds::getRepossessionByLenderTextArea,
                "selectedMandatoryGroundsCONTAINS\"REPOSSESSION_BY_LENDER\""
            )
            // Ground 3
            .readonly(ReasonForGrounds::getHolidayLetLabel,
                      "selectedMandatoryGroundsCONTAINS\"HOLIDAY_LET\"")
            .readonly(ReasonForGrounds::getHolidayLetGroundsLabel,
                      "selectedMandatoryGroundsCONTAINS\"HOLIDAY_LET\"")
            .mandatory(ReasonForGrounds::getHolidayLetTextArea,
                       "selectedMandatoryGroundsCONTAINS\"HOLIDAY_LET\"")
            // Ground 4
            .readonly(ReasonForGrounds::getStudentLetLabel,
                      "selectedMandatoryGroundsCONTAINS\"STUDENT_LET\"")
            .readonly(ReasonForGrounds::getStudentLetGroundsLabel,
                      "selectedMandatoryGroundsCONTAINS\"STUDENT_LET\"")
            .mandatory(ReasonForGrounds::getStudentLetTextArea,
                       "selectedMandatoryGroundsCONTAINS\"STUDENT_LET\"")
            // Ground 5
            .readonly(
                ReasonForGrounds::getMinisterOfReligionLabel,
                "selectedMandatoryGroundsCONTAINS\"MINISTER_OF_RELIGION\""
            )
            .readonly(
                ReasonForGrounds::getMinisterOfReligionGroundsLabel,
                "selectedMandatoryGroundsCONTAINS\"MINISTER_OF_RELIGION\""
            )
            .mandatory(
                ReasonForGrounds::getMinisterOfReligionTextArea,
                "selectedMandatoryGroundsCONTAINS\"MINISTER_OF_RELIGION\""
            )
            // Ground 6
            .readonly(ReasonForGrounds::getRedevelopmentLabel,
                      "selectedMandatoryGroundsCONTAINS\"REDEVELOPMENT\"")
            .readonly(
                ReasonForGrounds::getRedevelopmentGroundsLabel,
                "selectedMandatoryGroundsCONTAINS\"REDEVELOPMENT\""
            )
            .mandatory(ReasonForGrounds::getRedevelopmentTextArea,
                       "selectedMandatoryGroundsCONTAINS\"REDEVELOPMENT\"")
            // Ground 7
            .readonly(ReasonForGrounds::getDeathOfTenantLabel,
                      "selectedMandatoryGroundsCONTAINS\"DEATH_OF_TENANT\"")
            .readonly(
                ReasonForGrounds::getDeathOfTenantGroundsLabel,
                "selectedMandatoryGroundsCONTAINS\"DEATH_OF_TENANT\""
            )
            .mandatory(
                ReasonForGrounds::getDeathOfTenantTextArea,
                "selectedMandatoryGroundsCONTAINS\"DEATH_OF_TENANT\""
            )
            // Ground 7A
            .readonly(
                ReasonForGrounds::getAntisocialBehaviourLabel,
                "selectedMandatoryGroundsCONTAINS\"ANTISOCIAL_BEHAVIOUR\""
            )
            .readonly(
                ReasonForGrounds::getAntisocialBehaviourGroundsLabel,
                "selectedMandatoryGroundsCONTAINS\"ANTISOCIAL_BEHAVIOUR\""
            )
            .mandatory(
                ReasonForGrounds::getAntisocialBehaviourTextArea,
                "selectedMandatoryGroundsCONTAINS\"ANTISOCIAL_BEHAVIOUR\""
            )
            // Ground 7B
            .readonly(ReasonForGrounds::getNoRightToRentLabel,
                      "selectedMandatoryGroundsCONTAINS\"NO_RIGHT_TO_RENT\"")
            .readonly(
                ReasonForGrounds::getNoRightToRentGroundsLabel,
                "selectedMandatoryGroundsCONTAINS\"NO_RIGHT_TO_RENT\""
            )
            .mandatory(
                ReasonForGrounds::getNoRightToRentTextArea,
                "selectedMandatoryGroundsCONTAINS\"NO_RIGHT_TO_RENT\""
            )
            // Ground 8
            .readonly(
                ReasonForGrounds::getSeriousRentArrearsLabel,
                "selectedMandatoryGroundsCONTAINS\"SERIOUS_RENT_ARREARS\""
            )
            .readonly(
                ReasonForGrounds::getSeriousRentArrearsGroundsLabel,
                "selectedMandatoryGroundsCONTAINS\"SERIOUS_RENT_ARREARS\""
            )
            .mandatory(
                ReasonForGrounds::getSeriousRentArrearsTextArea,
                "selectedMandatoryGroundsCONTAINS\"SERIOUS_RENT_ARREARS\""
            )
            // Ground 9
            .readonly(
                ReasonForGrounds::getSuitableAlternativeAccommodationLabel,
                "selectedDiscretionaryGroundsCONTAINS\"SUITABLE_ALTERNATIVE_ACCOMMODATION\""
            )
            .readonly(
                ReasonForGrounds::getSuitableAlternativeAccommodationGroundsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"SUITABLE_ALTERNATIVE_ACCOMMODATION\""
            )
            .mandatory(
                ReasonForGrounds::getSuitableAlternativeAccommodationTextArea,
                "selectedDiscretionaryGroundsCONTAINS\"SUITABLE_ALTERNATIVE_ACCOMMODATION\""
            )
            // Ground 10
            .readonly(ReasonForGrounds::getRentArrearsLabel,
                      "selectedDiscretionaryGroundsCONTAINS\"RENT_ARREARS\"")
            .readonly(
                ReasonForGrounds::getRentArrearsGroundsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"RENT_ARREARS\""
            )
            .mandatory(ReasonForGrounds::getRentArrearsTextArea,
                       "selectedDiscretionaryGroundsCONTAINS\"RENT_ARREARS\"")
            // Ground 11
            .readonly(
                ReasonForGrounds::getPersistentDelayInPayingRentLabel,
                "selectedDiscretionaryGroundsCONTAINS\"PERSISTENT_DELAY_IN_PAYING_RENT\""
            )
            .readonly(
                ReasonForGrounds::getPersistentDelayInPayingRentGroundsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"PERSISTENT_DELAY_IN_PAYING_RENT\""
            )
            .mandatory(
                ReasonForGrounds::getPersistentDelayInPayingRentTextArea,
                "selectedDiscretionaryGroundsCONTAINS\"PERSISTENT_DELAY_IN_PAYING_RENT\""
            )
            // Ground 12
            .readonly(
                ReasonForGrounds::getBreachOfTenancyConditionsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"BREACH_OF_TENANCY_CONDITIONS\""
            )
            .readonly(
                ReasonForGrounds::getBreachOfTenancyConditionsGroundsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"BREACH_OF_TENANCY_CONDITIONS\""
            )
            .mandatory(
                ReasonForGrounds::getBreachOfTenancyConditionsTextArea,
                "selectedDiscretionaryGroundsCONTAINS\"BREACH_OF_TENANCY_CONDITIONS\""
            )
            // Ground 13
            .readonly(
                ReasonForGrounds::getPropertyDeteriorationLabel,
                "selectedDiscretionaryGroundsCONTAINS\"PROPERTY_DETERIORATION\""
            )
            .readonly(
                ReasonForGrounds::getPropertyDeteriorationGroundsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"PROPERTY_DETERIORATION\""
            )
            .mandatory(
                ReasonForGrounds::getPropertyDeteriorationTextArea,
                "selectedDiscretionaryGroundsCONTAINS\"PROPERTY_DETERIORATION\""
            )
            // Ground 14
            .readonly(
                ReasonForGrounds::getNuisanceOrIllegalUseLabel,
                "selectedDiscretionaryGroundsCONTAINS\"NUISANCE_OR_ILLEGAL_USE\""
            )
            .readonly(
                ReasonForGrounds::getNuisanceOrIllegalUseGroundsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"NUISANCE_OR_ILLEGAL_USE\""
            )
            .mandatory(
                ReasonForGrounds::getNuisanceOrIllegalUseTextArea,
                "selectedDiscretionaryGroundsCONTAINS\"NUISANCE_OR_ILLEGAL_USE\""
            )
            // Ground 14A
            .readonly(
                ReasonForGrounds::getDomesticViolenceLabel,
                "selectedDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE\""
            )
            .readonly(
                ReasonForGrounds::getDomesticViolenceGroundsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE\""
            )
            .mandatory(
                ReasonForGrounds::getDomesticViolenceTextArea,
                "selectedDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE\""
            )
            // Ground 14ZA
            .readonly(
                ReasonForGrounds::getOffenceDuringRiotLabel,
                "selectedDiscretionaryGroundsCONTAINS\"OFFENCE_DURING_RIOT\""
            )
            .readonly(
                ReasonForGrounds::getOffenceDuringRiotGroundsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"OFFENCE_DURING_RIOT\""
            )
            .mandatory(
                ReasonForGrounds::getOffenceDuringRiotTextArea,
                "selectedDiscretionaryGroundsCONTAINS\"OFFENCE_DURING_RIOT\""
            )
            // Ground 15
            .readonly(
                ReasonForGrounds::getFurnitureDeteriorationLabel,
                "selectedDiscretionaryGroundsCONTAINS\"FURNITURE_DETERIORATION\""
            )
            .readonly(
                ReasonForGrounds::getFurnitureDeteriorationGroundsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"FURNITURE_DETERIORATION\""
            )
            .mandatory(
                ReasonForGrounds::getFurnitureDeteriorationTextArea,
                "selectedDiscretionaryGroundsCONTAINS\"FURNITURE_DETERIORATION\""
            )
            // Ground 16
            .readonly(
                ReasonForGrounds::getLandlordEmployeeLabel,
                "selectedDiscretionaryGroundsCONTAINS\"LANDLORD_EMPLOYEE\""
            )
            .readonly(
                ReasonForGrounds::getLandlordEmployeeGroundsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"LANDLORD_EMPLOYEE\""
            )
            .mandatory(
                ReasonForGrounds::getLandlordEmployeeTextArea,
                "selectedDiscretionaryGroundsCONTAINS\"LANDLORD_EMPLOYEE\""
            )
            // Ground 17
            .readonly(
                ReasonForGrounds::getFalseStatementLabel,
                "selectedDiscretionaryGroundsCONTAINS\"FALSE_STATEMENT\""
            )
            .readonly(
                ReasonForGrounds::getFalseStatementGroundsLabel,
                "selectedDiscretionaryGroundsCONTAINS\"FALSE_STATEMENT\""
            )
            .mandatory(
                ReasonForGrounds::getFalseStatementTextArea,
                "selectedDiscretionaryGroundsCONTAINS\"FALSE_STATEMENT\""
            );

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }

}
