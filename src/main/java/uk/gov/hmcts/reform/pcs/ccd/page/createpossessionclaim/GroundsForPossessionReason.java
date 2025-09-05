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
            .readonly(PCSCase::getSelectedNoRentArrearsMandatoryGrounds, NEVER_SHOW)
            .readonly(PCSCase::getSelectedNoRentArrearsDiscretionaryGrounds, NEVER_SHOW)
            .complex(PCSCase::getReasonForGrounds)
            // Ground 1
            .readonly(
                ReasonForGrounds::getOwnerOccupierLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"OWNER_OCCUPIER\""
            )
            .readonly(
                ReasonForGrounds::getOwnerOccupierGroundsLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"OWNER_OCCUPIER\""
            )
            .mandatory(
                ReasonForGrounds::getOwnerOccupierTextArea,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"OWNER_OCCUPIER\""
            )
            // Ground 2
            .readonly(
                ReasonForGrounds::getRepossessionByLenderLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"REPOSSESSION_BY_LENDER\""
            )
            .readonly(
                ReasonForGrounds::getRepossessionByLenderGroundsLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"REPOSSESSION_BY_LENDER\""
            )
            .mandatory(
                ReasonForGrounds::getRepossessionByLenderTextArea,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"REPOSSESSION_BY_LENDER\""
            )
            // Ground 3
            .readonly(
                ReasonForGrounds::getHolidayLetLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"HOLIDAY_LET\""
            )
            .readonly(
                ReasonForGrounds::getHolidayLetGroundsLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"HOLIDAY_LET\""
            )
            .mandatory(
                ReasonForGrounds::getHolidayLetTextArea,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"HOLIDAY_LET\""
            )
            // Ground 4
            .readonly(
                ReasonForGrounds::getStudentLetLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"STUDENT_LET\""
            )
            .readonly(
                ReasonForGrounds::getStudentLetGroundsLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"STUDENT_LET\""
            )
            .mandatory(
                ReasonForGrounds::getStudentLetTextArea,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"STUDENT_LET\""
            )
            // Ground 5
            .readonly(
                ReasonForGrounds::getMinisterOfReligionLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"MINISTER_OF_RELIGION\""
            )
            .readonly(
                ReasonForGrounds::getMinisterOfReligionGroundsLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"MINISTER_OF_RELIGION\""
            )
            .mandatory(
                ReasonForGrounds::getMinisterOfReligionTextArea,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"MINISTER_OF_RELIGION\""
            )
            // Ground 6
            .readonly(
                ReasonForGrounds::getRedevelopmentLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"REDEVELOPMENT\""
            )
            .readonly(
                ReasonForGrounds::getRedevelopmentGroundsLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"REDEVELOPMENT\""
            )
            .mandatory(
                ReasonForGrounds::getRedevelopmentTextArea,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"REDEVELOPMENT\""
            )
            // Ground 7
            .readonly(
                ReasonForGrounds::getDeathOfTenantLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"DEATH_OF_TENANT\""
            )
            .readonly(
                ReasonForGrounds::getDeathOfTenantGroundsLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"DEATH_OF_TENANT\""
            )
            .mandatory(
                ReasonForGrounds::getDeathOfTenantTextArea,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"DEATH_OF_TENANT\""
            )
            // Ground 7A
            .readonly(
                ReasonForGrounds::getAntisocialBehaviourLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"ANTISOCIAL_BEHAVIOUR\""
            )
            .readonly(
                ReasonForGrounds::getAntisocialBehaviourGroundsLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"ANTISOCIAL_BEHAVIOUR\""
            )
            .mandatory(
                ReasonForGrounds::getAntisocialBehaviourTextArea,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"ANTISOCIAL_BEHAVIOUR\""
            )
            // Ground 7B
            .readonly(
                ReasonForGrounds::getNoRightToRentLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"NO_RIGHT_TO_RENT\""
            )
            .readonly(
                ReasonForGrounds::getNoRightToRentGroundsLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"NO_RIGHT_TO_RENT\""
            )
            .mandatory(
                ReasonForGrounds::getNoRightToRentTextArea,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"NO_RIGHT_TO_RENT\""
            )
            // Ground 8
            .readonly(
                ReasonForGrounds::getSeriousRentArrearsLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"SERIOUS_RENT_ARREARS\""
            )
            .readonly(
                ReasonForGrounds::getSeriousRentArrearsGroundsLabel,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"SERIOUS_RENT_ARREARS\""
            )
            .mandatory(
                ReasonForGrounds::getSeriousRentArrearsTextArea,
                "selectedNoRentArrearsMandatoryGroundsCONTAINS\"SERIOUS_RENT_ARREARS\""
            )
            // Ground 9
            .readonly(
                ReasonForGrounds::getSuitableAlternativeAccommodationLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"SUITABLE_ALTERNATIVE_ACCOMMODATION\""
            )
            .readonly(
                ReasonForGrounds::getSuitableAlternativeAccommodationGroundsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"SUITABLE_ALTERNATIVE_ACCOMMODATION\""
            )
            .mandatory(
                ReasonForGrounds::getSuitableAlternativeAccommodationTextArea,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"SUITABLE_ALTERNATIVE_ACCOMMODATION\""
            )
            // Ground 10
            .readonly(
                ReasonForGrounds::getRentArrearsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"RENT_ARREARS\""
            )
            .readonly(
                ReasonForGrounds::getRentArrearsGroundsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"RENT_ARREARS\""
            )
            .mandatory(
                ReasonForGrounds::getRentArrearsTextArea,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"RENT_ARREARS\""
            )
            // Ground 11
            .readonly(
                ReasonForGrounds::getPersistentDelayInPayingRentLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"PERSISTENT_DELAY_IN_PAYING_RENT\""
            )
            .readonly(
                ReasonForGrounds::getPersistentDelayInPayingRentGroundsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"PERSISTENT_DELAY_IN_PAYING_RENT\""
            )
            .mandatory(
                ReasonForGrounds::getPersistentDelayInPayingRentTextArea,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"PERSISTENT_DELAY_IN_PAYING_RENT\""
            )
            // Ground 12
            .readonly(
                ReasonForGrounds::getBreachOfTenancyConditionsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"BREACH_OF_TENANCY_CONDITIONS\""
            )
            .readonly(
                ReasonForGrounds::getBreachOfTenancyConditionsGroundsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"BREACH_OF_TENANCY_CONDITIONS\""
            )
            .mandatory(
                ReasonForGrounds::getBreachOfTenancyConditionsTextArea,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"BREACH_OF_TENANCY_CONDITIONS\""
            )
            // Ground 13
            .readonly(
                ReasonForGrounds::getPropertyDeteriorationLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"PROPERTY_DETERIORATION\""
            )
            .readonly(
                ReasonForGrounds::getPropertyDeteriorationGroundsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"PROPERTY_DETERIORATION\""
            )
            .mandatory(
                ReasonForGrounds::getPropertyDeteriorationTextArea,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"PROPERTY_DETERIORATION\""
            )
            // Ground 14
            .readonly(
                ReasonForGrounds::getNuisanceOrIllegalUseLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"NUISANCE_OR_ILLEGAL_USE\""
            )
            .readonly(
                ReasonForGrounds::getNuisanceOrIllegalUseGroundsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"NUISANCE_OR_ILLEGAL_USE\""
            )
            .mandatory(
                ReasonForGrounds::getNuisanceOrIllegalUseTextArea,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"NUISANCE_OR_ILLEGAL_USE\""
            )
            // Ground 14A
            .readonly(
                ReasonForGrounds::getDomesticViolenceLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE\""
            )
            .readonly(
                ReasonForGrounds::getDomesticViolenceGroundsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE\""
            )
            .mandatory(
                ReasonForGrounds::getDomesticViolenceTextArea,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE\""
            )
            // Ground 14ZA
            .readonly(
                ReasonForGrounds::getOffenceDuringRiotLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"OFFENCE_DURING_RIOT\""
            )
            .readonly(
                ReasonForGrounds::getOffenceDuringRiotGroundsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"OFFENCE_DURING_RIOT\""
            )
            .mandatory(
                ReasonForGrounds::getOffenceDuringRiotTextArea,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"OFFENCE_DURING_RIOT\""
            )
            // Ground 15
            .readonly(
                ReasonForGrounds::getFurnitureDeteriorationLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"FURNITURE_DETERIORATION\""
            )
            .readonly(
                ReasonForGrounds::getFurnitureDeteriorationGroundsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"FURNITURE_DETERIORATION\""
            )
            .mandatory(
                ReasonForGrounds::getFurnitureDeteriorationTextArea,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"FURNITURE_DETERIORATION\""
            )
            // Ground 16
            .readonly(
                ReasonForGrounds::getLandlordEmployeeLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"LANDLORD_EMPLOYEE\""
            )
            .readonly(
                ReasonForGrounds::getLandlordEmployeeGroundsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"LANDLORD_EMPLOYEE\""
            )
            .mandatory(
                ReasonForGrounds::getLandlordEmployeeTextArea,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"LANDLORD_EMPLOYEE\""
            )
            // Ground 17
            .readonly(
                ReasonForGrounds::getFalseStatementLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"FALSE_STATEMENT\""
            )
            .readonly(
                ReasonForGrounds::getFalseStatementGroundsLabel,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"FALSE_STATEMENT\""
            )
            .mandatory(
                ReasonForGrounds::getFalseStatementTextArea,
                "selectedNoRentArrearsDiscretionaryGroundsCONTAINS\"FALSE_STATEMENT\""
            );
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }

}
