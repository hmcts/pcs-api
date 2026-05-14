package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherNoGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class ClaimGroundSummaryBuilder {

    public List<ListValue<ClaimGroundSummary>> buildClaimGroundSummariesFromDraft(PCSCase draftCaseData) {
        List<ListValue<ClaimGroundSummary>> summaries = new ArrayList<>();

        OccupationLicenceDetailsWales occupationLicenceDetailsWales = draftCaseData.getOccupationLicenceDetailsWales();
        OccupationLicenceTypeWales occupationLicenceType = occupationLicenceDetailsWales == null
            ? null : occupationLicenceDetailsWales.getOccupationLicenceTypeWales();

        if (occupationLicenceType == OccupationLicenceTypeWales.SECURE_CONTRACT) {
            Optional.ofNullable(draftCaseData.getSecureContractGroundsForPossessionWales()).ifPresent(selected -> {
                addGrounds(summaries, selected.getMandatoryGrounds());
                addGrounds(summaries, selected.getDiscretionaryGrounds());
                addGrounds(summaries, selected.getEstateManagementGrounds());
            });

            return summaries;
        } else if (occupationLicenceType == OccupationLicenceTypeWales.STANDARD_CONTRACT
            || occupationLicenceType == OccupationLicenceTypeWales.OTHER) {
            Optional.ofNullable(draftCaseData.getGroundsForPossessionWales()).ifPresent(selected -> {
                addGrounds(summaries, selected.getMandatoryGrounds());
                addGrounds(summaries, selected.getDiscretionaryGrounds());
                addGrounds(summaries, selected.getEstateManagementGrounds());
            });

            return summaries;
        }

        TenancyLicenceDetails tenancyLicenceDetails = draftCaseData.getTenancyLicenceDetails();
        TenancyLicenceType tenancyType = tenancyLicenceDetails == null
            ? null : tenancyLicenceDetails.getTypeOfTenancyLicence();

        if (tenancyType == TenancyLicenceType.ASSURED_TENANCY) {
            if (draftCaseData.getClaimDueToRentArrears() == YesOrNo.YES) {
                addAssuredRentArrearsGrounds(summaries, draftCaseData);
            } else if (draftCaseData.getClaimDueToRentArrears() == YesOrNo.NO) {
                Optional.ofNullable(draftCaseData.getNoRentArrearsGroundsOptions()).ifPresent(selected -> {
                    addGrounds(summaries, selected.getMandatoryGrounds(), draftCaseData);
                    addGrounds(summaries, selected.getDiscretionaryGrounds(), draftCaseData);
                });
            }
        } else if (tenancyType == TenancyLicenceType.SECURE_TENANCY
            || tenancyType == TenancyLicenceType.FLEXIBLE_TENANCY) {
            Optional.ofNullable(draftCaseData.getSecureOrFlexiblePossessionGrounds()).ifPresent(selected -> {
                addGrounds(summaries, selected.getSecureOrFlexibleMandatoryGrounds(), draftCaseData);
                addGrounds(summaries, selected.getSecureOrFlexibleDiscretionaryGrounds(), draftCaseData);
                addGrounds(summaries, selected.getSecureAntisocialAdditionalGrounds(), draftCaseData);
                addGrounds(summaries, selected.getSecureOrFlexibleMandatoryGroundsAlt(), draftCaseData);
                addGrounds(summaries, selected.getSecureOrFlexibleDiscretionaryGroundsAlt(), draftCaseData);
            });
        } else if (tenancyType == TenancyLicenceType.INTRODUCTORY_TENANCY
            || tenancyType == TenancyLicenceType.DEMOTED_TENANCY
            || tenancyType == TenancyLicenceType.OTHER) {
            addIntroductoryDemotedOrOtherGrounds(summaries, draftCaseData);
        }

        return summaries;
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
                    .build())
                .map(summary -> ListValue.<ClaimGroundSummary>builder().value(summary).build())
                .forEach(summaries::add);
        }
    }

    private String getDraftReason(PCSCase draftCaseData, PossessionGroundEnum ground) {
        return switch (((Enum<?>) ground).name()) {
            case "OWNER_OCCUPIER_GROUND1" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getOwnerOccupierReason();
            case "REPOSSESSION_GROUND2" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getRepossessionByLenderReason();
            case "HOLIDAY_LET_GROUND3" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getHolidayLetReason();
            case "STUDENT_LET_GROUND4" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getStudentLetReason();
            case "MINISTER_RELIGION_GROUND5" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getMinisterOfReligionReason();
            case "REDEVELOPMENT_GROUND6" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getRedevelopmentReason();
            case "DEATH_OF_TENANT_GROUND7" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getDeathOfTenantReason();
            case "ANTISOCIAL_BEHAVIOUR_GROUND7A" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getAntisocialBehaviourReason();
            case "NO_RIGHT_TO_RENT_GROUND7B" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getNoRightToRentReason();
            case "RENT_ARREARS_GROUND10", "PERSISTENT_DELAY_GROUND11", "SERIOUS_RENT_ARREARS_GROUND8" -> null;
            case "ALTERNATIVE_ACCOMMODATION_GROUND9" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getSuitableAltAccommodationReason();
            case "BREACH_TENANCY_GROUND12" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getBreachOfTenancyConditionsReason();
            case "DETERIORATION_PROPERTY_GROUND13" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getPropertyDeteriorationReason();
            case "NUISANCE_ANNOYANCE_GROUND14" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getNuisanceAnnoyanceReason();
            case "DOMESTIC_VIOLENCE_GROUND14A" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getDomesticViolenceReason();
            case "OFFENCE_RIOT_GROUND14ZA" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getOffenceDuringRiotReason();
            case "DETERIORATION_FURNITURE_GROUND15" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getFurnitureDeteriorationReason();
            case "EMPLOYEE_LANDLORD_GROUND16" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getEmployeeOfLandlordReason();
            case "FALSE_STATEMENT_GROUND17" -> getRentArrearsGroundsReasons(draftCaseData) == null
                ? null : getRentArrearsGroundsReasons(draftCaseData).getTenancyByFalseStatementReason();
            case "ANTI_SOCIAL" -> getAntiSocialReason(draftCaseData);
            case "BREACH_OF_THE_TENANCY" -> getIntroductoryDemotedOtherGroundReason(draftCaseData) == null
                ? null : getIntroductoryDemotedOtherGroundReason(draftCaseData).getBreachOfTheTenancyGround();
            case "ABSOLUTE_GROUNDS" -> getIntroductoryDemotedOtherGroundReason(draftCaseData) == null
                ? null : getIntroductoryDemotedOtherGroundReason(draftCaseData).getAbsoluteGrounds();
            case "OTHER" -> getIntroductoryDemotedOtherGroundReason(draftCaseData) == null
                ? null : getIntroductoryDemotedOtherGroundReason(draftCaseData).getOtherGround();
            case "NO_GROUNDS" -> getIntroductoryDemotedOtherGroundReason(draftCaseData) == null
                ? null : getIntroductoryDemotedOtherGroundReason(draftCaseData).getNoGrounds();
            case "RENT_ARREARS_OR_BREACH_OF_TENANCY" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getBreachOfTenancyGround();
            case "NUISANCE_OR_IMMORAL_USE" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getNuisanceOrImmoralUseGround();
            case "DOMESTIC_VIOLENCE" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getDomesticViolenceGround();
            case "RIOT_OFFENCE" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getRiotOffenceGround();
            case "PROPERTY_DETERIORATION" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getPropertyDeteriorationGround();
            case "FURNITURE_DETERIORATION" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getFurnitureDeteriorationGround();
            case "TENANCY_OBTAINED_BY_FALSE_STATEMENT" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getTenancyByFalseStatementGround();
            case "PREMIUM_PAID_MUTUAL_EXCHANGE" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getPremiumMutualExchangeGround();
            case "UNREASONABLE_CONDUCT_TIED_ACCOMMODATION" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getUnreasonableConductGround();
            case "REFUSAL_TO_MOVE_BACK" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getRefusalToMoveBackGround();
            case "S84A_CONDITION_1" -> getSection84ACondition1Reason(draftCaseData);
            case "S84A_CONDITION_2" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getAntiSocialCondition2OfS84AGround();
            case "S84A_CONDITION_3" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getAntiSocialCondition3OfS84AGround();
            case "S84A_CONDITION_4" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getAntiSocialCondition4OfS84AGround();
            case "S84A_CONDITION_5" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getAntiSocialCondition5OfS84AGround();
            case "OVERCROWDING" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getOvercrowdingGround();
            case "LANDLORD_WORKS" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getLandlordWorksGround();
            case "PROPERTY_SOLD" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getPropertySoldGround();
            case "CHARITABLE_LANDLORD" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getCharitableLandlordGround();
            case "TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getTiedAccommodationGround();
            case "ADAPTED_ACCOMMODATION" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getAdaptedAccommodationGround();
            case "HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getHousingAssocSpecialGround();
            case "SPECIAL_NEEDS_ACCOMMODATION" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getSpecialNeedsAccommodationGround();
            case "UNDER_OCCUPYING_AFTER_SUCCESSION" -> getSecureOrFlexibleReason(draftCaseData) == null
                ? null : getSecureOrFlexibleReason(draftCaseData).getUnderOccupancySuccessionGround();
            default -> null;
        };
    }

    private String getSection84ACondition1Reason(PCSCase draftCaseData) {
        SecureOrFlexibleGroundsReasons reasons = draftCaseData.getSecureOrFlexibleGroundsReasons();
        return reasons == null ? null : reasons.getAntiSocialCondition1OfS84AGround();
    }

    private String getAntiSocialReason(PCSCase draftCaseData) {
        SecureOrFlexibleGroundsReasons secureOrFlexibleReasons = getSecureOrFlexibleReason(draftCaseData);
        if (secureOrFlexibleReasons != null) {
            return secureOrFlexibleReasons.getAntiSocialGround();
        }

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
