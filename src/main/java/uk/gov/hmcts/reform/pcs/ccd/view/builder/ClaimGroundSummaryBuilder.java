package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherNoGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.NoRentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureAntisocialAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

@Component
public class ClaimGroundSummaryBuilder {

    private static final Map<PossessionGroundEnum, BiFunction<ClaimGroundSummaryBuilder, PCSCase, String>>
        ASSURED_DRAFT_REASON_LOOKUP = Map.ofEntries(
            Map.entry(AssuredAdditionalOtherGround.OTHER,
                ClaimGroundSummaryBuilder::getAssuredOtherGroundReason),
            assuredReasonEntry(AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1,
                RentArrearsGroundsReasons::getOwnerOccupierReason),
            assuredReasonEntry(AssuredMandatoryGround.REPOSSESSION_GROUND2,
                RentArrearsGroundsReasons::getRepossessionByLenderReason),
            assuredReasonEntry(AssuredMandatoryGround.HOLIDAY_LET_GROUND3,
                RentArrearsGroundsReasons::getHolidayLetReason),
            assuredReasonEntry(AssuredMandatoryGround.STUDENT_LET_GROUND4,
                RentArrearsGroundsReasons::getStudentLetReason),
            assuredReasonEntry(AssuredMandatoryGround.MINISTER_RELIGION_GROUND5,
                RentArrearsGroundsReasons::getMinisterOfReligionReason),
            assuredReasonEntry(AssuredMandatoryGround.REDEVELOPMENT_GROUND6,
                RentArrearsGroundsReasons::getRedevelopmentReason),
            assuredReasonEntry(AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7,
                RentArrearsGroundsReasons::getDeathOfTenantReason),
            assuredReasonEntry(AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A,
                RentArrearsGroundsReasons::getAntisocialBehaviourReason),
            assuredReasonEntry(AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B,
                RentArrearsGroundsReasons::getNoRightToRentReason),
            assuredReasonEntry(AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9,
                RentArrearsGroundsReasons::getSuitableAltAccommodationReason),
            assuredReasonEntry(AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12,
                RentArrearsGroundsReasons::getBreachOfTenancyConditionsReason),
            assuredReasonEntry(AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13,
                RentArrearsGroundsReasons::getPropertyDeteriorationReason),
            assuredReasonEntry(AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14,
                RentArrearsGroundsReasons::getNuisanceAnnoyanceReason),
            assuredReasonEntry(AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A,
                RentArrearsGroundsReasons::getDomesticViolenceReason),
            assuredReasonEntry(AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA,
                RentArrearsGroundsReasons::getOffenceDuringRiotReason),
            assuredReasonEntry(AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15,
                RentArrearsGroundsReasons::getFurnitureDeteriorationReason),
            assuredReasonEntry(AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16,
                RentArrearsGroundsReasons::getEmployeeOfLandlordReason),
            assuredReasonEntry(AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17,
                RentArrearsGroundsReasons::getTenancyByFalseStatementReason)
        );

    private static final Map<PossessionGroundEnum, BiFunction<ClaimGroundSummaryBuilder, PCSCase, String>>
        SECURE_OR_FLEXIBLE_DRAFT_REASON_LOOKUP = Map.ofEntries(
            Map.entry(SecureAntisocialAdditionalGrounds.S84A_CONDITION_1,
                ClaimGroundSummaryBuilder::getSection84ACondition1Reason),
            secureOrFlexibleReasonEntry(SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY,
                SecureOrFlexibleGroundsReasons::getBreachOfTenancyGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE,
                SecureOrFlexibleGroundsReasons::getNuisanceOrImmoralUseGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleDiscretionaryGrounds.DOMESTIC_VIOLENCE,
                SecureOrFlexibleGroundsReasons::getDomesticViolenceGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE,
                SecureOrFlexibleGroundsReasons::getRiotOffenceGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleDiscretionaryGrounds.PROPERTY_DETERIORATION,
                SecureOrFlexibleGroundsReasons::getPropertyDeteriorationGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleDiscretionaryGrounds.FURNITURE_DETERIORATION,
                SecureOrFlexibleGroundsReasons::getFurnitureDeteriorationGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleDiscretionaryGrounds.TENANCY_OBTAINED_BY_FALSE_STATEMENT,
                SecureOrFlexibleGroundsReasons::getTenancyByFalseStatementGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleDiscretionaryGrounds.PREMIUM_PAID_MUTUAL_EXCHANGE,
                SecureOrFlexibleGroundsReasons::getPremiumMutualExchangeGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleDiscretionaryGrounds.UNREASONABLE_CONDUCT_TIED_ACCOMMODATION,
                SecureOrFlexibleGroundsReasons::getUnreasonableConductGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleDiscretionaryGrounds.REFUSAL_TO_MOVE_BACK,
                SecureOrFlexibleGroundsReasons::getRefusalToMoveBackGround),
            secureOrFlexibleReasonEntry(SecureAntisocialAdditionalGrounds.S84A_CONDITION_2,
                SecureOrFlexibleGroundsReasons::getAntiSocialCondition2OfS84AGround),
            secureOrFlexibleReasonEntry(SecureAntisocialAdditionalGrounds.S84A_CONDITION_3,
                SecureOrFlexibleGroundsReasons::getAntiSocialCondition3OfS84AGround),
            secureOrFlexibleReasonEntry(SecureAntisocialAdditionalGrounds.S84A_CONDITION_4,
                SecureOrFlexibleGroundsReasons::getAntiSocialCondition4OfS84AGround),
            secureOrFlexibleReasonEntry(SecureAntisocialAdditionalGrounds.S84A_CONDITION_5,
                SecureOrFlexibleGroundsReasons::getAntiSocialCondition5OfS84AGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.OVERCROWDING,
                SecureOrFlexibleGroundsReasons::getOvercrowdingGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.LANDLORD_WORKS,
                SecureOrFlexibleGroundsReasons::getLandlordWorksGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD,
                SecureOrFlexibleGroundsReasons::getPropertySoldGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.CHARITABLE_LANDLORD,
                SecureOrFlexibleGroundsReasons::getCharitableLandlordGround),
            secureOrFlexibleReasonEntry(
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE,
                SecureOrFlexibleGroundsReasons::getTiedAccommodationGround),
            secureOrFlexibleReasonEntry(SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ADAPTED_ACCOMMODATION,
                SecureOrFlexibleGroundsReasons::getAdaptedAccommodationGround),
            secureOrFlexibleReasonEntry(
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES,
                SecureOrFlexibleGroundsReasons::getHousingAssocSpecialGround),
            secureOrFlexibleReasonEntry(
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.SPECIAL_NEEDS_ACCOMMODATION,
                SecureOrFlexibleGroundsReasons::getSpecialNeedsAccommodationGround),
            secureOrFlexibleReasonEntry(
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.UNDER_OCCUPYING_AFTER_SUCCESSION,
                SecureOrFlexibleGroundsReasons::getUnderOccupancySuccessionGround)
        );

    private static final Map<PossessionGroundEnum, Function<GroundsReasonsWales, String>>
        WALES_DRAFT_REASON_LOOKUP = Map.ofEntries(
            walesReasonEntry(MandatoryGroundWales.FAILURE_TO_GIVE_UP_POSSESSION_S170,
                GroundsReasonsWales::getFailToGiveUpS170Reason),
            walesReasonEntry(MandatoryGroundWales.LANDLORD_NOTICE_PERIODIC_S178,
                GroundsReasonsWales::getLandlordNoticePeriodicS178Reason),
            walesReasonEntry(MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181,
                GroundsReasonsWales::getSeriousArrearsPeriodicS181Reason),
            walesReasonEntry(MandatoryGroundWales.LANDLORD_NOTICE_FT_END_S186,
                GroundsReasonsWales::getLandlordNoticeFtEndS186Reason),
            walesReasonEntry(MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187,
                GroundsReasonsWales::getSeriousArrearsFixedTermS187Reason),
            walesReasonEntry(MandatoryGroundWales.FAIL_TO_GIVE_UP_BREAK_NOTICE_S191,
                GroundsReasonsWales::getFailToGiveUpBreakNoticeS191Reason),
            walesReasonEntry(MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199,
                GroundsReasonsWales::getLandlordBreakClauseS199Reason),
            walesReasonEntry(MandatoryGroundWales.CONVERTED_FIXED_TERM_SCH12_25B2,
                GroundsReasonsWales::getConvertedFixedTermSch1225B2Reason),
            walesReasonEntry(DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157,
                GroundsReasonsWales::getOtherBreachSection157Reason),
            walesReasonEntry(EstateManagementGroundsWales.BUILDING_WORKS,
                GroundsReasonsWales::getBuildingWorksReason),
            walesReasonEntry(EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES,
                GroundsReasonsWales::getRedevelopmentSchemesReason),
            walesReasonEntry(EstateManagementGroundsWales.CHARITIES,
                GroundsReasonsWales::getCharitiesReason),
            walesReasonEntry(EstateManagementGroundsWales.DISABLED_SUITABLE_DWELLING,
                GroundsReasonsWales::getDisabledSuitableDwellingReason),
            walesReasonEntry(EstateManagementGroundsWales.HOUSING_ASSOCIATIONS_AND_TRUSTS,
                GroundsReasonsWales::getHousingAssociationsAndTrustsReason),
            walesReasonEntry(EstateManagementGroundsWales.SPECIAL_NEEDS_DWELLINGS,
                GroundsReasonsWales::getSpecialNeedsDwellingsReason),
            walesReasonEntry(EstateManagementGroundsWales.RESERVE_SUCCESSORS,
                GroundsReasonsWales::getReserveSuccessorsReason),
            walesReasonEntry(EstateManagementGroundsWales.JOINT_CONTRACT_HOLDERS,
                GroundsReasonsWales::getJointContractHoldersReason),
            walesReasonEntry(EstateManagementGroundsWales.OTHER_ESTATE_MANAGEMENT_REASONS,
                GroundsReasonsWales::getOtherEstateManagementReasonsReason),
            walesReasonEntry(SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_S170,
                GroundsReasonsWales::getSecureFailureToGiveUpPossessionSection170Reason),
            walesReasonEntry(SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S186,
                GroundsReasonsWales::getSecureLandlordNoticeSection186Reason),
            walesReasonEntry(SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_S191,
                GroundsReasonsWales::getSecureFailureToGiveUpPossessionSection191Reason),
            walesReasonEntry(SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S199,
                GroundsReasonsWales::getSecureLandlordNoticeSection199Reason),
            walesReasonEntry(SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT_S157,
                GroundsReasonsWales::getSecureOtherBreachOfContractReason)
        );

    private static final Map<PossessionGroundEnum, Function<GroundsReasonsWales, String>>
        WALES_SECURE_ESTATE_MANAGEMENT_DRAFT_REASON_LOOKUP = Map.ofEntries(
            walesReasonEntry(EstateManagementGroundsWales.BUILDING_WORKS,
                GroundsReasonsWales::getSecureBuildingWorksReason),
            walesReasonEntry(EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES,
                GroundsReasonsWales::getSecureRedevelopmentSchemesReason),
            walesReasonEntry(EstateManagementGroundsWales.CHARITIES,
                GroundsReasonsWales::getSecureCharitiesReason),
            walesReasonEntry(EstateManagementGroundsWales.DISABLED_SUITABLE_DWELLING,
                GroundsReasonsWales::getSecureDisabledSuitableDwellingReason),
            walesReasonEntry(EstateManagementGroundsWales.HOUSING_ASSOCIATIONS_AND_TRUSTS,
                GroundsReasonsWales::getSecureHousingAssociationsAndTrustsReason),
            walesReasonEntry(EstateManagementGroundsWales.SPECIAL_NEEDS_DWELLINGS,
                GroundsReasonsWales::getSecureSpecialNeedsDwellingsReason),
            walesReasonEntry(EstateManagementGroundsWales.RESERVE_SUCCESSORS,
                GroundsReasonsWales::getSecureReserveSuccessorsReason),
            walesReasonEntry(EstateManagementGroundsWales.JOINT_CONTRACT_HOLDERS,
                GroundsReasonsWales::getSecureJointContractHoldersReason),
            walesReasonEntry(EstateManagementGroundsWales.OTHER_ESTATE_MANAGEMENT_REASONS,
                GroundsReasonsWales::getSecureOtherEstateManagementReasonsReason)
        );

    public List<ListValue<ClaimGroundSummary>> buildClaimGroundSummariesFromDraft(PCSCase draftCaseData) {
        List<ListValue<ClaimGroundSummary>> summaries = new ArrayList<>();

        OccupationLicenceDetailsWales occupationLicenceDetailsWales = draftCaseData.getOccupationLicenceDetailsWales();
        OccupationLicenceTypeWales occupationLicenceType = occupationLicenceDetailsWales == null
            ? null : occupationLicenceDetailsWales.getOccupationLicenceTypeWales();

        if (occupationLicenceType == OccupationLicenceTypeWales.SECURE_CONTRACT) {
            Optional.ofNullable(draftCaseData.getSecureContractGroundsForPossessionWales()).ifPresent(selected -> {
                addGrounds(summaries, selected.getMandatoryGrounds(), draftCaseData);
                addGrounds(summaries, selected.getDiscretionaryGrounds(), draftCaseData);
                addSecureWalesEstateManagementGrounds(summaries, selected.getEstateManagementGrounds(), draftCaseData);
            });

            return summaries;
        } else if (occupationLicenceType == OccupationLicenceTypeWales.STANDARD_CONTRACT
            || occupationLicenceType == OccupationLicenceTypeWales.OTHER) {
            Optional.ofNullable(draftCaseData.getGroundsForPossessionWales()).ifPresent(selected -> {
                addGrounds(summaries, selected.getMandatoryGrounds(), draftCaseData);
                addGrounds(summaries, selected.getDiscretionaryGrounds(), draftCaseData);
                addGrounds(summaries, selected.getEstateManagementGrounds(), draftCaseData);
            });

            return summaries;
        }

        addTenancyLicenceGrounds(summaries, draftCaseData);

        return summaries;
    }

    private void addTenancyLicenceGrounds(List<ListValue<ClaimGroundSummary>> summaries,
                                          PCSCase draftCaseData) {
        TenancyLicenceType tenancyType = getTenancyType(draftCaseData);
        if (tenancyType == TenancyLicenceType.ASSURED_TENANCY) {
            if (draftCaseData.getClaimDueToRentArrears() == YesOrNo.YES) {
                addAssuredRentArrearsGrounds(summaries, draftCaseData);
            } else if (draftCaseData.getClaimDueToRentArrears() == YesOrNo.NO) {
                Optional.ofNullable(draftCaseData.getNoRentArrearsGroundsOptions()).ifPresent(selected -> {
                    addGrounds(summaries, selected.getMandatoryGrounds(), draftCaseData);
                    addGrounds(summaries, selected.getDiscretionaryGrounds(), draftCaseData);
                    addGrounds(summaries, selected.getOtherGround(), draftCaseData);
                });
            }
        } else if (isSecureOrFlexibleTenancy(tenancyType)) {
            Optional.ofNullable(draftCaseData.getSecureOrFlexiblePossessionGrounds()).ifPresent(selected -> {
                addGrounds(summaries, selected.getSecureOrFlexibleMandatoryGrounds(), draftCaseData);
                addGrounds(summaries, selected.getSecureOrFlexibleDiscretionaryGrounds(), draftCaseData);
                addGrounds(summaries, selected.getSecureAntisocialAdditionalGrounds(), draftCaseData);
                addGrounds(summaries, selected.getSecureOrFlexibleMandatoryGroundsAlt(), draftCaseData);
                addGrounds(summaries, selected.getSecureOrFlexibleDiscretionaryGroundsAlt(), draftCaseData);
            });
        } else if (isIntroductoryDemotedOrOtherTenancy(tenancyType)) {
            addIntroductoryDemotedOrOtherGrounds(summaries, draftCaseData);
        }
    }

    private TenancyLicenceType getTenancyType(PCSCase draftCaseData) {
        TenancyLicenceDetails tenancyLicenceDetails = draftCaseData.getTenancyLicenceDetails();
        return tenancyLicenceDetails == null ? null : tenancyLicenceDetails.getTypeOfTenancyLicence();
    }

    private boolean isSecureOrFlexibleTenancy(TenancyLicenceType tenancyType) {
        return tenancyType == TenancyLicenceType.SECURE_TENANCY
            || tenancyType == TenancyLicenceType.FLEXIBLE_TENANCY;
    }

    private boolean isIntroductoryDemotedOrOtherTenancy(TenancyLicenceType tenancyType) {
        return tenancyType == TenancyLicenceType.INTRODUCTORY_TENANCY
            || tenancyType == TenancyLicenceType.DEMOTED_TENANCY
            || tenancyType == TenancyLicenceType.OTHER;
    }

    private void addAssuredRentArrearsGrounds(List<ListValue<ClaimGroundSummary>> summaries,
                                             PCSCase draftCaseData) {
        Optional.ofNullable(draftCaseData.getAssuredRentArrearsPossessionGrounds()).ifPresent(selected -> {
            if (!CollectionUtils.isEmpty(selected.getRentArrearsGrounds())) {
                addGrounds(summaries, selected.getRentArrearsGrounds().stream()
                    .map(this::mapAssuredRentArrearsGround)
                    .toList(), draftCaseData);
            }
            if (!CollectionUtils.isEmpty(selected.getAdditionalMandatoryGrounds())) {
                addGrounds(summaries, selected.getAdditionalMandatoryGrounds().stream()
                    .map(ground -> AssuredMandatoryGround.valueOf(ground.name()))
                    .toList(), draftCaseData);
            }
            if (!CollectionUtils.isEmpty(selected.getAdditionalDiscretionaryGrounds())) {
                addGrounds(summaries, selected.getAdditionalDiscretionaryGrounds().stream()
                    .map(ground -> AssuredDiscretionaryGround.valueOf(ground.name()))
                    .toList(), draftCaseData);
            }
            addGrounds(summaries, selected.getAdditionalOtherGround(), draftCaseData);
        });
    }

    private void addIntroductoryDemotedOrOtherGrounds(List<ListValue<ClaimGroundSummary>> summaries,
                                                     PCSCase draftCaseData) {
        IntroductoryDemotedOtherGroundsForPossession selected =
            draftCaseData.getIntroductoryDemotedOrOtherGroundsForPossession();

        if (selected == null) {
            return;
        }

        if (selected.getHasIntroductoryDemotedOtherGroundsForPossession() == VerticalYesNo.NO) {
            addGrounds(summaries, Set.of(IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS), draftCaseData);
            return;
        }

        addGrounds(summaries, selected.getIntroductoryDemotedOrOtherGrounds(), draftCaseData);
    }

    private void addGrounds(List<ListValue<ClaimGroundSummary>> summaries,
                            Set<? extends PossessionGroundEnum> grounds) {
        if (!CollectionUtils.isEmpty(grounds)) {
            addGrounds(summaries, List.copyOf(grounds));
        }
    }

    private void addGrounds(List<ListValue<ClaimGroundSummary>> summaries,
                            Set<? extends PossessionGroundEnum> grounds,
                            PCSCase draftCaseData) {
        if (!CollectionUtils.isEmpty(grounds)) {
            addGrounds(summaries, List.copyOf(grounds), draftCaseData);
        }
    }

    private void addGrounds(List<ListValue<ClaimGroundSummary>> summaries,
                            List<? extends PossessionGroundEnum> grounds) {
        if (!CollectionUtils.isEmpty(grounds)) {
            grounds.stream()
                .map(ground -> ClaimGroundSummary.builder().label(ground.getLabel()).build())
                .map(summary -> ListValue.<ClaimGroundSummary>builder().value(summary).build())
                .forEach(summaries::add);
        }
    }

    private void addGrounds(List<ListValue<ClaimGroundSummary>> summaries,
                            List<? extends PossessionGroundEnum> grounds,
                            PCSCase draftCaseData) {
        if (!CollectionUtils.isEmpty(grounds)) {
            grounds.stream()
                .map(ground -> ClaimGroundSummary.builder()
                    .code(((Enum<?>) ground).name())
                    .label(ground.getLabel())
                    .reason(getDraftReason(draftCaseData, ground))
                    .description(getDescription(draftCaseData, ground))
                    .build())
                .map(summary -> ListValue.<ClaimGroundSummary>builder().value(summary).build())
                .forEach(summaries::add);
        }
    }

    private void addSecureWalesEstateManagementGrounds(List<ListValue<ClaimGroundSummary>> summaries,
                                                       Set<? extends PossessionGroundEnum> grounds,
                                                       PCSCase draftCaseData) {
        if (!CollectionUtils.isEmpty(grounds)) {
            addSecureWalesEstateManagementGrounds(summaries, List.copyOf(grounds), draftCaseData);
        }
    }

    private void addSecureWalesEstateManagementGrounds(List<ListValue<ClaimGroundSummary>> summaries,
                                                       List<? extends PossessionGroundEnum> grounds,
                                                       PCSCase draftCaseData) {
        if (!CollectionUtils.isEmpty(grounds)) {
            grounds.stream()
                .map(ground -> ClaimGroundSummary.builder()
                    .code(((Enum<?>) ground).name())
                    .label(ground.getLabel())
                    .reason(getSecureWalesEstateManagementDraftReason(draftCaseData, ground))
                    .build())
                .map(summary -> ListValue.<ClaimGroundSummary>builder().value(summary).build())
                .forEach(summaries::add);
        }
    }

    private String getDescription(PCSCase draftCaseData, PossessionGroundEnum ground) {
        if (ground == IntroductoryDemotedOrOtherGrounds.OTHER) {
            IntroductoryDemotedOtherGroundsForPossession otherGroundsForPossession =
                draftCaseData.getIntroductoryDemotedOrOtherGroundsForPossession();
            return otherGroundsForPossession != null ? otherGroundsForPossession.getOtherGroundDescription() : null;
        }

        if (ground == AssuredAdditionalOtherGround.OTHER) {
            if (draftCaseData.getClaimDueToRentArrears() == YesOrNo.YES) {
                AssuredRentArrearsPossessionGrounds grounds = draftCaseData.getAssuredRentArrearsPossessionGrounds();
                return grounds != null ? grounds.getAdditionalOtherGroundDescription() : null;
            } else {
                AssuredNoArrearsPossessionGrounds grounds = draftCaseData.getNoRentArrearsGroundsOptions();
                return grounds != null ? grounds.getOtherGroundDescription() : null;
            }
        }

        return null;
    }

    private String getDraftReason(PCSCase draftCaseData, PossessionGroundEnum ground) {
        String reason = getAssuredDraftReason(draftCaseData, ground);
        if (reason != null) {
            return reason;
        }

        reason = getIntroductoryDemotedOrOtherDraftReason(draftCaseData, ground);
        if (reason != null) {
            return reason;
        }

        reason = getSecureOrFlexibleDraftReason(draftCaseData, ground);
        if (reason != null) {
            return reason;
        }

        return getWalesDraftReason(draftCaseData, ground);
    }

    private String getAssuredDraftReason(PCSCase draftCaseData, PossessionGroundEnum ground) {
        return Optional.ofNullable(ASSURED_DRAFT_REASON_LOOKUP.get(ground))
            .map(reasonAccessor -> reasonAccessor.apply(this, draftCaseData))
            .orElse(null);
    }

    private String getAssuredOtherGroundReason(PCSCase draftCaseData) {
        RentArrearsGroundsReasons rentArrearsGroundsReasons = getRentArrearsGroundsReasons(draftCaseData);
        if (rentArrearsGroundsReasons != null && rentArrearsGroundsReasons.getOtherGroundReason() != null) {
            return rentArrearsGroundsReasons.getOtherGroundReason();
        }

        NoRentArrearsGroundsReasons noRentArrearsGroundsReasons =
            draftCaseData.getNoRentArrearsGroundsReasons();
        return noRentArrearsGroundsReasons == null ? null : noRentArrearsGroundsReasons.getOtherGround();
    }

    private String getIntroductoryDemotedOrOtherDraftReason(PCSCase draftCaseData, PossessionGroundEnum ground) {
        IntroductoryDemotedOtherGroundReason reasons = getIntroductoryDemotedOtherGroundReason(draftCaseData);

        if (ground == IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL) {
            return getAntiSocialReason(draftCaseData);
        }

        if (reasons == null) {
            return null;
        }

        if (ground == IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY) {
            return reasons.getBreachOfTheTenancyGround();
        } else if (ground == IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS) {
            return reasons.getAbsoluteGrounds();
        } else if (ground == IntroductoryDemotedOrOtherGrounds.OTHER) {
            return reasons.getOtherGround();
        } else if (ground == IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS) {
            return reasons.getNoGrounds();
        }

        return null;
    }

    private String getSecureOrFlexibleDraftReason(PCSCase draftCaseData, PossessionGroundEnum ground) {
        BiFunction<ClaimGroundSummaryBuilder, PCSCase, String> reasonAccessor =
            SECURE_OR_FLEXIBLE_DRAFT_REASON_LOOKUP.get(ground);
        return reasonAccessor == null ? null : reasonAccessor.apply(this, draftCaseData);
    }

    private static Map.Entry<PossessionGroundEnum, BiFunction<ClaimGroundSummaryBuilder, PCSCase, String>>
        assuredReasonEntry(PossessionGroundEnum ground,
                           Function<RentArrearsGroundsReasons, String> reasonAccessor) {
        return Map.entry(ground, (builder, draftCaseData) -> Optional
            .ofNullable(builder.getRentArrearsGroundsReasons(draftCaseData))
            .map(reasonAccessor)
            .orElse(null));
    }

    private static Map.Entry<PossessionGroundEnum, BiFunction<ClaimGroundSummaryBuilder, PCSCase, String>>
        secureOrFlexibleReasonEntry(PossessionGroundEnum ground,
                                    Function<SecureOrFlexibleGroundsReasons, String> reasonAccessor) {
        return Map.entry(ground, (builder, draftCaseData) -> {
            SecureOrFlexibleGroundsReasons reasons = builder.getSecureOrFlexibleReason(draftCaseData);
            return reasons == null ? null : reasonAccessor.apply(reasons);
        });
    }

    private static Map.Entry<PossessionGroundEnum, Function<GroundsReasonsWales, String>>
        walesReasonEntry(PossessionGroundEnum ground,
                         Function<GroundsReasonsWales, String> reasonAccessor) {
        return Map.entry(ground, reasonAccessor);
    }

    private String getWalesDraftReason(PCSCase draftCaseData, PossessionGroundEnum ground) {
        return getWalesDraftReason(draftCaseData, ground, WALES_DRAFT_REASON_LOOKUP);
    }

    private String getWalesDraftReason(
        PCSCase draftCaseData,
        PossessionGroundEnum ground,
        Map<PossessionGroundEnum, Function<GroundsReasonsWales, String>> reasonLookup
    ) {
        GroundsReasonsWales reasons = draftCaseData.getGroundsReasonsWales();
        return Optional.ofNullable(reasonLookup.get(ground))
            .map(reasonAccessor -> Optional.ofNullable(reasons)
                .map(reasonAccessor)
                .orElse(null))
            .orElse(null);
    }

    private String getSecureWalesEstateManagementDraftReason(PCSCase draftCaseData, PossessionGroundEnum ground) {
        return getWalesDraftReason(draftCaseData, ground, WALES_SECURE_ESTATE_MANAGEMENT_DRAFT_REASON_LOOKUP);
    }

    private String getSection84ACondition1Reason(PCSCase draftCaseData) {
        SecureOrFlexibleGroundsReasons reasons = draftCaseData.getSecureOrFlexibleGroundsReasons();
        return reasons == null ? null : reasons.getAntiSocialCondition1OfS84AGround();
    }

    private String getAntiSocialReason(PCSCase draftCaseData) {
        IntroductoryDemotedOtherGroundReason introductoryDemotedOtherReason =
            getIntroductoryDemotedOtherGroundReason(draftCaseData);
        return introductoryDemotedOtherReason == null
            ? null : introductoryDemotedOtherReason.getAntiSocialBehaviourGround();
    }

    private SecureOrFlexibleGroundsReasons getSecureOrFlexibleReason(PCSCase draftCaseData) {
        return draftCaseData.getSecureOrFlexibleGroundsReasons();
    }

    private RentArrearsGroundsReasons getRentArrearsGroundsReasons(PCSCase draftCaseData) {
        return draftCaseData.getRentArrearsGroundsReasons();
    }

    private IntroductoryDemotedOtherGroundReason getIntroductoryDemotedOtherGroundReason(PCSCase draftCaseData) {
        return draftCaseData.getIntroductoryDemotedOtherGroundReason();
    }

    private PossessionGroundEnum mapAssuredRentArrearsGround(AssuredRentArrearsGround ground) {
        if (ground == AssuredRentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8) {
            return AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8;
        }

        return AssuredDiscretionaryGround.valueOf(ground.name());
    }

}
