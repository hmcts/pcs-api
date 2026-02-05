package uk.gov.hmcts.reform.pcs.ccd.service.ground;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales.RENT_ARREARS_S157;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181;

@Service
public class WalesStandardClaimGroundService {

    public List<ClaimGroundEntity> createClaimGroundEntities(PCSCase pcsCase) {
        GroundsForPossessionWales possessionGrounds = pcsCase.getGroundsForPossessionWales();

        Set<MandatoryGroundWales> mandatoryGrounds
            = Objects.requireNonNullElse(possessionGrounds.getMandatoryGrounds(), Set.of());
        Set<DiscretionaryGroundWales> discretionaryGrounds
            = Objects.requireNonNullElse(possessionGrounds.getDiscretionaryGrounds(), Set.of());
        Set<EstateManagementGroundsWales> estateGrounds
            = Objects.requireNonNullElse(possessionGrounds.getEstateManagementGrounds(), Set.of());

        GroundsReasonsWales reasons = pcsCase.getGroundsReasonsWales();

        List<ClaimGroundEntity> claimGroundEntities = new ArrayList<>();
        addMandatoryGroundEntities(claimGroundEntities, mandatoryGrounds, reasons);
        addDiscretionaryGroundEntities(claimGroundEntities, discretionaryGrounds, reasons);

        if (discretionaryGrounds.contains(ESTATE_MANAGEMENT_GROUNDS_S160)) {
            addEstateManagementGroundEntities(claimGroundEntities, estateGrounds, reasons);
        }

        return claimGroundEntities;
    }

    private static void addMandatoryGroundEntities(List<ClaimGroundEntity> claimGroundEntities,
                                                   Set<MandatoryGroundWales> mandatoryGrounds,
                                                   GroundsReasonsWales reasons) {

        for (MandatoryGroundWales ground : mandatoryGrounds) {
            String reasonText = switch (ground) {
                case FAILURE_TO_GIVE_UP_POSSESSION_S170 -> reasons.getFailToGiveUpS170Reason();
                case LANDLORD_NOTICE_PERIODIC_S178 -> reasons.getLandlordNoticePeriodicS178Reason();
                case SERIOUS_ARREARS_PERIODIC_S181 -> reasons.getSeriousArrearsPeriodicS181Reason();
                case LANDLORD_NOTICE_FT_END_S186 -> reasons.getLandlordNoticeFtEndS186Reason();
                case SERIOUS_ARREARS_FIXED_TERM_S187 -> reasons.getSeriousArrearsFixedTermS187Reason();
                case FAIL_TO_GIVE_UP_BREAK_NOTICE_S191 -> reasons.getFailToGiveUpBreakNoticeS191Reason();
                case LANDLORD_BREAK_CLAUSE_S199 -> reasons.getLandlordBreakClauseS199Reason();
                case CONVERTED_FIXED_TERM_SCH12_25B2 -> reasons.getConvertedFixedTermSch1225B2Reason();
            };

            boolean isRentArrears
                = (ground == SERIOUS_ARREARS_PERIODIC_S181 || ground == SERIOUS_ARREARS_FIXED_TERM_S187);

            claimGroundEntities.add(ClaimGroundEntity.builder()
                             .category(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY)
                             .code(ground.name())
                             .reason(reasonText)
                             .isRentArrears(isRentArrears)
                             .build());
        }
    }

    private static void addDiscretionaryGroundEntities(List<ClaimGroundEntity> claimGroundEntities,
                                                       Set<DiscretionaryGroundWales> discretionaryGrounds,
                                                       GroundsReasonsWales reasons) {

        for (DiscretionaryGroundWales ground : discretionaryGrounds) {
            String reasonText = switch (ground) {
                case RENT_ARREARS_S157, ANTISOCIAL_BEHAVIOUR_S157, ESTATE_MANAGEMENT_GROUNDS_S160 -> null;
                case OTHER_BREACH_OF_CONTRACT_S157 -> reasons.getOtherBreachSection157Reason();
            };

            boolean isRentArrears = (ground == RENT_ARREARS_S157);

            claimGroundEntities.add(ClaimGroundEntity.builder()
                                        .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
                                        .code(ground.name())
                                        .reason(reasonText)
                                        .isRentArrears(isRentArrears)
                                        .build());
        }
    }


    private static void addEstateManagementGroundEntities(List<ClaimGroundEntity> claimGroundEntities,
                                                          Set<EstateManagementGroundsWales> estateGrounds,
                                                          GroundsReasonsWales reasons) {

        for (EstateManagementGroundsWales ground : estateGrounds) {
            String reasonText = switch (ground) {
                case BUILDING_WORKS -> reasons.getBuildingWorksReason();
                case REDEVELOPMENT_SCHEMES -> reasons.getRedevelopmentSchemesReason();
                case CHARITIES -> reasons.getCharitiesReason();
                case DISABLED_SUITABLE_DWELLING -> reasons.getDisabledSuitableDwellingReason();
                case HOUSING_ASSOCIATIONS_AND_TRUSTS -> reasons.getHousingAssociationsAndTrustsReason();
                case SPECIAL_NEEDS_DWELLINGS -> reasons.getSpecialNeedsDwellingsReason();
                case RESERVE_SUCCESSORS -> reasons.getReserveSuccessorsReason();
                case JOINT_CONTRACT_HOLDERS -> reasons.getJointContractHoldersReason();
                case OTHER_ESTATE_MANAGEMENT_REASONS -> reasons.getOtherEstateManagementReasonsReason();
            };

            claimGroundEntities.add(ClaimGroundEntity.builder()
                                        .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                                        .code(ground.name())
                                        .reason(reasonText)
                                        .isRentArrears(false)
                                        .build());
        }
    }

}
