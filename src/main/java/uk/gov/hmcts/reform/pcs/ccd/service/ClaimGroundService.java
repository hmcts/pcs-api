package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ClaimGroundService {
    public List<ClaimGroundEntity> getGroundsWithReason(PCSCase pcsCase) {
        Set<NoRentArrearsMandatoryGrounds> noRentArrearsMandatoryGrounds = pcsCase
            .getNoRentArrearsMandatoryGroundsOptions();
        Set<NoRentArrearsDiscretionaryGrounds> noRentArrearsDiscretionaryGrounds = pcsCase
            .getNoRentArrearsDiscretionaryGroundsOptions();
        NoRentArrearsReasonForGrounds grounds = pcsCase.getNoRentArrearsReasonForGrounds();

        List<ClaimGroundEntity> entities = new ArrayList<>();

        for (NoRentArrearsMandatoryGrounds ground : noRentArrearsMandatoryGrounds) {
            String reasonText = switch (ground) {
                case OWNER_OCCUPIER -> grounds.getOwnerOccupierTextArea();
                case REPOSSESSION_BY_LENDER -> grounds.getRepossessionByLenderTextArea();
                case HOLIDAY_LET -> grounds.getHolidayLetTextArea();
                case STUDENT_LET -> grounds.getStudentLetTextArea();
                case MINISTER_OF_RELIGION -> grounds.getMinisterOfReligionTextArea();
                case REDEVELOPMENT -> grounds.getRedevelopmentTextArea();
                case DEATH_OF_TENANT -> grounds.getDeathOfTenantTextArea();
                case ANTISOCIAL_BEHAVIOUR -> grounds.getAntisocialBehaviourTextArea();
                case NO_RIGHT_TO_RENT -> grounds.getNoRightToRentTextArea();
                case SERIOUS_RENT_ARREARS -> grounds.getSeriousRentArrearsTextArea();
            };

            entities.add(ClaimGroundEntity.builder()
                             .groundsId(ground.name())
                             .claimsReasonText(reasonText)
                             .build());
        }

        for (NoRentArrearsDiscretionaryGrounds ground : noRentArrearsDiscretionaryGrounds) {
            String reasonText = switch (ground) {
                case SUITABLE_ACCOM -> grounds.getSuitableAccomTextArea();
                case RENT_ARREARS -> grounds.getRentArrearsTextArea();
                case RENT_PAYMENT_DELAY -> grounds.getRentPaymentDelayTextArea();
                case BREACH_OF_TENANCY_CONDITIONS -> grounds.getBreachOfTenancyConditionsTextArea();
                case PROPERTY_DETERIORATION -> grounds.getPropertyDeteriorationTextArea();
                case NUISANCE_OR_ILLEGAL_USE -> grounds.getNuisanceOrIllegalUseTextArea();
                case DOMESTIC_VIOLENCE -> grounds.getDomesticViolenceTextArea();
                case OFFENCE_DURING_RIOT -> grounds.getOffenceDuringRiotTextArea();
                case FURNITURE_DETERIORATION -> grounds.getFurnitureDeteriorationTextArea();
                case LANDLORD_EMPLOYEE -> grounds.getLandlordEmployeeTextArea();
                case FALSE_STATEMENT -> grounds.getFalseStatementTextArea();
            };

            entities.add(ClaimGroundEntity.builder()
                             .groundsId(ground.name())
                             .claimsReasonText(reasonText)
                             .build());
        }
        return entities;
    }
}
