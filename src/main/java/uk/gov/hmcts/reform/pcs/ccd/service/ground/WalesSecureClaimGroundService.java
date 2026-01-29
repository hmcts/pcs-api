package uk.gov.hmcts.reform.pcs.ccd.service.ground;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS_S160;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales.RENT_ARREARS_S157;


@Service
public class WalesSecureClaimGroundService {

    public List<ClaimGroundEntity> createClaimGroundEntities(PCSCase pcsCase) {
        SecureContractGroundsForPossessionWales possessionGrounds
            = pcsCase.getSecureContractGroundsForPossessionWales();

        Set<SecureContractMandatoryGroundsWales> mandatoryGrounds
            = Objects.requireNonNullElse(possessionGrounds.getMandatoryGrounds(), Set.of());
        Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds
            = Objects.requireNonNullElse(possessionGrounds.getDiscretionaryGrounds(), Set.of());
        Set<EstateManagementGroundsWales> estateGrounds
            = Objects.requireNonNullElse(possessionGrounds.getEstateManagementGrounds(), Set.of());

        GroundsReasonsWales reasons = pcsCase.getGroundsReasonsWales();

        List<ClaimGroundEntity> claimGroundEntities = new ArrayList<>();
        addSecureMandatoryGroundEntities(claimGroundEntities, mandatoryGrounds, reasons);
        addSecureDiscretionaryGroundEntities(claimGroundEntities, discretionaryGrounds, reasons);

        if (discretionaryGrounds.contains(ESTATE_MANAGEMENT_GROUNDS_S160)) {
            addSecureEstateManagementGroundEntities(claimGroundEntities, estateGrounds, reasons);
        }

        return claimGroundEntities;
    }

    private static void addSecureMandatoryGroundEntities(List<ClaimGroundEntity> entities,
                                                         Set<SecureContractMandatoryGroundsWales> mandatoryGrounds,
                                                         GroundsReasonsWales reasons) {

        for (SecureContractMandatoryGroundsWales ground : mandatoryGrounds) {
            String reasonText = switch (ground) {
                case FAILURE_TO_GIVE_UP_POSSESSION_S170 -> reasons.getSecureFailureToGiveUpPossessionSection170Reason();
                case LANDLORD_NOTICE_S186 -> reasons.getSecureLandlordNoticeSection186Reason();
                case FAILURE_TO_GIVE_UP_POSSESSION_S191 -> reasons.getSecureFailureToGiveUpPossessionSection191Reason();
                case LANDLORD_NOTICE_S199 -> reasons.getSecureLandlordNoticeSection199Reason();
            };

            entities.add(ClaimGroundEntity.builder()
                             .category(ClaimGroundCategory.WALES_SECURE_MANDATORY)
                             .code(ground.name())
                             .reason(reasonText)
                             .isRentArrears(false)
                             .build());
        }
    }


    private static void addSecureDiscretionaryGroundEntities(List<ClaimGroundEntity> claimGroundEntities,
                                                             Set<SecureContractDiscretionaryGroundsWales>
                                                                 discretionaryGrounds,
                                                             GroundsReasonsWales reasons) {

        for (SecureContractDiscretionaryGroundsWales ground : discretionaryGrounds) {
            String reasonText = switch (ground) {
                case RENT_ARREARS_S157, ANTISOCIAL_BEHAVIOUR_S157, ESTATE_MANAGEMENT_GROUNDS_S160 -> null;
                case OTHER_BREACH_OF_CONTRACT_S157 -> reasons.getSecureOtherBreachOfContractReason();
            };

            boolean isRentArrears = (ground == RENT_ARREARS_S157);

            claimGroundEntities.add(ClaimGroundEntity.builder()
                                        .category(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY)
                                        .code(ground.name())
                                        .reason(reasonText)
                                        .isRentArrears(isRentArrears)
                                        .build());
        }
    }

    private static void addSecureEstateManagementGroundEntities(List<ClaimGroundEntity> claimGroundEntities,
                                                                Set<EstateManagementGroundsWales> estateGrounds,
                                                                GroundsReasonsWales reasons) {
        for (EstateManagementGroundsWales ground : estateGrounds) {
            String reasonText = switch (ground) {
                case BUILDING_WORKS -> reasons.getSecureBuildingWorksReason();
                case REDEVELOPMENT_SCHEMES -> reasons.getSecureRedevelopmentSchemesReason();
                case CHARITIES -> reasons.getSecureCharitiesReason();
                case DISABLED_SUITABLE_DWELLING -> reasons.getSecureDisabledSuitableDwellingReason();
                case HOUSING_ASSOCIATIONS_AND_TRUSTS -> reasons.getSecureHousingAssociationsAndTrustsReason();
                case SPECIAL_NEEDS_DWELLINGS -> reasons.getSecureSpecialNeedsDwellingsReason();
                case RESERVE_SUCCESSORS -> reasons.getSecureReserveSuccessorsReason();
                case JOINT_CONTRACT_HOLDERS -> reasons.getSecureJointContractHoldersReason();
                case OTHER_ESTATE_MANAGEMENT_REASONS -> reasons.getSecureOtherEstateManagementReasonsReason();
            };

            claimGroundEntities.add(ClaimGroundEntity.builder()
                                        .category(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT)
                                        .code(ground.name())
                                        .reason(reasonText)
                                        .isRentArrears(false)
                                        .build());
        }
    }

}
