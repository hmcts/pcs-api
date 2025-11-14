package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static feign.Util.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS;

@Service
public class ClaimGroundService {

    public List<ClaimGroundEntity> getGroundsWithReason(PCSCase pcsCase) {
        TenancyLicenceType tenancyLicenceType = pcsCase.getTypeOfTenancyLicence();

        if (tenancyLicenceType == null) {
            return Collections.emptyList();
        }

        return switch (tenancyLicenceType) {
            case ASSURED_TENANCY -> getAssuredTenancyGroundsWithReason(pcsCase);
            case INTRODUCTORY_TENANCY, DEMOTED_TENANCY, OTHER ->
                getIntroductoryDemotedOtherTenancyGroundsWithReason(pcsCase);
            case SECURE_TENANCY, FLEXIBLE_TENANCY ->
                getSecureFlexibleTenancyGroundsWithReason(pcsCase);
        };
    }

    private List<ClaimGroundEntity> getAssuredTenancyGroundsWithReason(PCSCase pcsCase) {

        if (pcsCase.getClaimDueToRentArrears() == YesOrNo.YES) {
            return assuredTenancyRentArrearsGroundsWithReason(pcsCase);
        } else {
            return assuredTenancyNoRentArrearsGroundsWithReason(pcsCase);
        }
    }

    private List<ClaimGroundEntity> assuredTenancyRentArrearsGroundsWithReason(PCSCase pcsCase) {

        Set<RentArrearsGround> rentArrearsGrounds = pcsCase.getRentArrearsGrounds();
        Set<RentArrearsMandatoryGrounds> rentArrearsMandatoryGrounds = pcsCase
            .getRentArrearsMandatoryGrounds();
        Set<RentArrearsDiscretionaryGrounds> rentArrearsDiscretionaryGrounds = pcsCase
            .getRentArrearsDiscretionaryGrounds();
        RentArrearsGroundsReasons grounds = pcsCase.getRentArrearsGroundsReasons();

        List<ClaimGroundEntity> entities = new ArrayList<>();

        if (rentArrearsMandatoryGrounds == null && rentArrearsDiscretionaryGrounds == null) {
            rentArrearsGrounds.forEach(rentArrearsGround -> {
                entities.add(ClaimGroundEntity.builder()
                                 .groundId(rentArrearsGround.name())
                                 .build());
            });
            return entities;
        }

        if (rentArrearsMandatoryGrounds != null) {
            for (RentArrearsMandatoryGrounds ground : rentArrearsMandatoryGrounds) {
                String reasonText = switch (ground) {
                    case OWNER_OCCUPIER_GROUND1 -> grounds.getOwnerOccupierReason();
                    case REPOSSESSION_GROUND2 -> grounds.getRepossessionByLenderReason();
                    case HOLIDAY_LET_GROUND3 -> grounds.getHolidayLetReason();
                    case STUDENT_LET_GROUND4 -> grounds.getStudentLetReason();
                    case MINISTER_RELIGION_GROUND5 -> grounds.getMinisterOfReligionReason();
                    case REDEVELOPMENT_GROUND6 -> grounds.getRedevelopmentReason();
                    case DEATH_OF_TENANT_GROUND7 -> grounds.getDeathOfTenantReason();
                    case ANTISOCIAL_BEHAVIOUR_GROUND7A -> grounds.getAntisocialBehaviourReason();
                    case NO_RIGHT_TO_RENT_GROUND7B -> grounds.getNoRightToRentReason();
                    case SERIOUS_RENT_ARREARS_GROUND8 -> null;
                };

                entities.add(ClaimGroundEntity.builder()
                                 .groundId(ground.name())
                                 .groundReason(reasonText)
                                 .build());
            }
        }

        if (rentArrearsDiscretionaryGrounds != null) {
            for (RentArrearsDiscretionaryGrounds ground : rentArrearsDiscretionaryGrounds) {
                String reasonText = switch (ground) {
                    case ALTERNATIVE_ACCOMMODATION_GROUND9 -> grounds.getSuitableAltAccommodationReason();
                    case RENT_ARREARS_GROUND10, PERSISTENT_DELAY_GROUND11 -> null;
                    case BREACH_TENANCY_GROUND12 -> grounds.getBreachOfTenancyConditionsReason();
                    case DETERIORATION_PROPERTY_GROUND13 -> grounds.getPropertyDeteriorationReason();
                    case NUISANCE_ANNOYANCE_GROUND14 -> grounds.getNuisanceAnnoyanceReason();
                    case DOMESTIC_VIOLENCE_GROUND14A -> grounds.getDomesticViolenceReason();
                    case OFFENCE_RIOT_GROUND14ZA -> grounds.getOffenceDuringRiotReason();
                    case DETERIORATION_FURNITURE_GROUND15 -> grounds.getFurnitureDeteriorationReason();
                    case EMPLOYEE_LANDLORD_GROUND16 -> grounds.getEmployeeOfLandlordReason();
                    case FALSE_STATEMENT_GROUND17 ->  grounds.getTenancyByFalseStatementReason();
                };

                entities.add(ClaimGroundEntity.builder()
                                 .groundId(ground.name())
                                 .groundReason(reasonText)
                                 .build());
            }
        }
        return entities;
    }

    private List<ClaimGroundEntity> assuredTenancyNoRentArrearsGroundsWithReason(PCSCase pcsCase) {

        Set<NoRentArrearsMandatoryGrounds> noRentArrearsMandatoryGrounds = pcsCase
                .getNoRentArrearsMandatoryGroundsOptions();
        Set<NoRentArrearsDiscretionaryGrounds> noRentArrearsDiscretionaryGrounds = pcsCase
                .getNoRentArrearsDiscretionaryGroundsOptions();
        NoRentArrearsReasonForGrounds grounds = pcsCase.getNoRentArrearsReasonForGrounds();

        List<ClaimGroundEntity> entities = new ArrayList<>();

        if (noRentArrearsMandatoryGrounds != null) {
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
                    case SERIOUS_RENT_ARREARS -> null;
                };

                entities.add(ClaimGroundEntity.builder()
                                 .groundId(ground.name())
                                 .groundReason(reasonText)
                                 .build());
            }
        }

        if (noRentArrearsDiscretionaryGrounds != null) {
            for (NoRentArrearsDiscretionaryGrounds ground : noRentArrearsDiscretionaryGrounds) {
                String reasonText = switch (ground) {
                    case SUITABLE_ACCOM -> grounds.getSuitableAccomTextArea();
                    case RENT_ARREARS, RENT_PAYMENT_DELAY -> null;
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
        }

        return entities;
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
            && isNotBlank(reasons.getNoGrounds())) {

            entities.add(
                ClaimGroundEntity.builder()
                    .groundId(NO_GROUNDS.name())
                    .groundReason(pcsCase.getIntroductoryDemotedOtherGroundReason().getNoGrounds())
                    .build());
        }

        return entities;
    }

    //TODO - Integrate Secure/Flexible grounds, currently being saved as JSONB
    private List<ClaimGroundEntity> getSecureFlexibleTenancyGroundsWithReason(PCSCase pcsCase) {
        return List.of();
    }

}
