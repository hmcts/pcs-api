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
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherNoGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureAntisocialAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

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
        SECURE_OR_FLEXIBLE_DRAFT_REASON_LOOKUP = Map.ofEntries(
            draftReasonEntry(SecureAntisocialAdditionalGrounds.S84A_CONDITION_1,
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
        String reason = getAssuredDraftReason(draftCaseData, ground);
        if (reason != null) {
            return reason;
        }

        reason = getIntroductoryDemotedOrOtherDraftReason(draftCaseData, ground);
        if (reason != null) {
            return reason;
        }

        return getSecureOrFlexibleDraftReason(draftCaseData, ground);
    }

    private String getAssuredDraftReason(PCSCase draftCaseData, PossessionGroundEnum ground) {
        RentArrearsGroundsReasons reasons = getRentArrearsGroundsReasons(draftCaseData);

        if (reasons == null) {
            return null;
        }

        if (ground == AssuredMandatoryGround.OWNER_OCCUPIER_GROUND1) {
            return reasons.getOwnerOccupierReason();
        } else if (ground == AssuredMandatoryGround.REPOSSESSION_GROUND2) {
            return reasons.getRepossessionByLenderReason();
        } else if (ground == AssuredMandatoryGround.HOLIDAY_LET_GROUND3) {
            return reasons.getHolidayLetReason();
        } else if (ground == AssuredMandatoryGround.STUDENT_LET_GROUND4) {
            return reasons.getStudentLetReason();
        } else if (ground == AssuredMandatoryGround.MINISTER_RELIGION_GROUND5) {
            return reasons.getMinisterOfReligionReason();
        } else if (ground == AssuredMandatoryGround.REDEVELOPMENT_GROUND6) {
            return reasons.getRedevelopmentReason();
        } else if (ground == AssuredMandatoryGround.DEATH_OF_TENANT_GROUND7) {
            return reasons.getDeathOfTenantReason();
        } else if (ground == AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A) {
            return reasons.getAntisocialBehaviourReason();
        } else if (ground == AssuredMandatoryGround.NO_RIGHT_TO_RENT_GROUND7B) {
            return reasons.getNoRightToRentReason();
        } else if (ground == AssuredDiscretionaryGround.ALTERNATIVE_ACCOMMODATION_GROUND9) {
            return reasons.getSuitableAltAccommodationReason();
        } else if (ground == AssuredDiscretionaryGround.BREACH_TENANCY_GROUND12) {
            return reasons.getBreachOfTenancyConditionsReason();
        } else if (ground == AssuredDiscretionaryGround.DETERIORATION_PROPERTY_GROUND13) {
            return reasons.getPropertyDeteriorationReason();
        } else if (ground == AssuredDiscretionaryGround.NUISANCE_ANNOYANCE_GROUND14) {
            return reasons.getNuisanceAnnoyanceReason();
        } else if (ground == AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A) {
            return reasons.getDomesticViolenceReason();
        } else if (ground == AssuredDiscretionaryGround.OFFENCE_RIOT_GROUND14ZA) {
            return reasons.getOffenceDuringRiotReason();
        } else if (ground == AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15) {
            return reasons.getFurnitureDeteriorationReason();
        } else if (ground == AssuredDiscretionaryGround.EMPLOYEE_LANDLORD_GROUND16) {
            return reasons.getEmployeeOfLandlordReason();
        } else if (ground == AssuredDiscretionaryGround.FALSE_STATEMENT_GROUND17) {
            return reasons.getTenancyByFalseStatementReason();
        }

        return null;
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
        draftReasonEntry(PossessionGroundEnum ground,
                         BiFunction<ClaimGroundSummaryBuilder, PCSCase, String> reasonAccessor) {
        return Map.entry(ground, reasonAccessor);
    }

    private static Map.Entry<PossessionGroundEnum, BiFunction<ClaimGroundSummaryBuilder, PCSCase, String>>
        secureOrFlexibleReasonEntry(PossessionGroundEnum ground,
                                    Function<SecureOrFlexibleGroundsReasons, String> reasonAccessor) {
        return Map.entry(ground, (builder, draftCaseData) -> {
            SecureOrFlexibleGroundsReasons reasons = builder.getSecureOrFlexibleReason(draftCaseData);
            return reasons == null ? null : reasonAccessor.apply(reasons);
        });
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
