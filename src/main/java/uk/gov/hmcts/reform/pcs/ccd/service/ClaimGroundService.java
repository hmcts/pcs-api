package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static feign.Util.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS;

@Service
public class ClaimGroundService {

    public List<ClaimGroundEntity> getGroundsWithReason(PCSCase pcsCase) {
        TenancyLicenceType tenancyLicenceType = pcsCase.getTypeOfTenancyLicence();

        return switch (tenancyLicenceType) {
            case ASSURED_TENANCY -> getAssuredTenancyGroundsWithReason(pcsCase);
            case INTRODUCTORY_TENANCY, DEMOTED_TENANCY, OTHER ->
                getIntroductoryDemotedOtherTenancyGroundsWithReason(pcsCase);
            case SECURE_TENANCY, FLEXIBLE_TENANCY ->
                getSecureFlexibleTenancyGroundsWithReason(pcsCase);
        };
    }

    private List<ClaimGroundEntity> getIntroductoryDemotedOtherTenancyGroundsWithReason(
          PCSCase pcsCase) {
        Set<IntroductoryDemotedOrOtherGrounds> introductoryDemotedOrOtherGrounds =
            pcsCase.getIntroductoryDemotedOrOtherGrounds();

        IntroductoryDemotedOtherGroundReason reasons = pcsCase.getIntroductoryDemotedOtherGroundReason();

        List<ClaimGroundEntity> entities = new ArrayList<>();
        if (introductoryDemotedOrOtherGrounds != null) {
            for (IntroductoryDemotedOrOtherGrounds ground : introductoryDemotedOrOtherGrounds) {
                String reasonText = switch (ground) {
                    case ABSOLUTE_GROUNDS -> reasons.getAbsoluteGrounds();
                    case ANTI_SOCIAL -> reasons.getAntiSocialBehaviourGround();
                    case BREACH_OF_THE_TENANCY -> reasons.getBreachOfTheTenancyGround();
                    case OTHER -> reasons.getOtherGround();
                    case RENT_ARREARS -> null;
                };

                String groundDescription = ground.equals(IntroductoryDemotedOrOtherGrounds.OTHER)
                        ? pcsCase.getOtherGroundDescription() : null;

                entities.add(
                        ClaimGroundEntity.builder()
                                .groundId(ground.name())
                                .groundReason(reasonText)
                                .groundDescription(groundDescription)
                                .build());
            }
        }
        if (pcsCase.getHasIntroductoryDemotedOtherGroundsForPossession() == VerticalYesNo.NO
            && isNotBlank(reasons.getAbsoluteGrounds())) {

            entities.add(
                ClaimGroundEntity.builder()
                    .groundId(ABSOLUTE_GROUNDS.name())
                    .groundReason(pcsCase.getIntroductoryDemotedOtherGroundReason().getAbsoluteGrounds())
                    .groundDescription(null)
                    .build());
        }

        return entities;
    }

    private List<ClaimGroundEntity> getAssuredTenancyGroundsWithReason(PCSCase pcsCase) {

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
                    .groundId(ground.name())
                    .groundReason(reasonText)
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
                    .groundId(ground.name())
                    .groundReason(reasonText)
                    .build());
        }
        return entities;
    }

    //TODO - Integrate Secure/Flexible grounds, currently being saved as JSONB
    private List<ClaimGroundEntity> getSecureFlexibleTenancyGroundsWithReason(PCSCase pcsCase) {
        return List.of();
    }

}
