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
import java.util.Objects;
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
                    addGrounds(summaries, selected.getOtherGround(), draftCaseData);
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
            if(!CollectionUtils.isEmpty(selected.getAdditionalOtherGround())) {
                addGrounds(summaries, selected.getAdditionalOtherGround().stream()
                    .map(ground -> AssuredAdditionalOtherGround.valueOf(ground.name()))
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
                    .description(getDescription(draftCaseData, ground))
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
        SecureOrFlexibleGroundsReasons reasons = getSecureOrFlexibleReason(draftCaseData);

        if (ground == SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL) {
            return getAntiSocialReason(draftCaseData);
        }

        if (ground == SecureAntisocialAdditionalGrounds.S84A_CONDITION_1) {
            return getSection84ACondition1Reason(draftCaseData);
        }

        if (reasons == null) {
            return null;
        }

        if (ground == SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY) {
            return reasons.getBreachOfTenancyGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE) {
            return reasons.getNuisanceOrImmoralUseGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGrounds.DOMESTIC_VIOLENCE) {
            return reasons.getDomesticViolenceGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE) {
            return reasons.getRiotOffenceGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGrounds.PROPERTY_DETERIORATION) {
            return reasons.getPropertyDeteriorationGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGrounds.FURNITURE_DETERIORATION) {
            return reasons.getFurnitureDeteriorationGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGrounds.TENANCY_OBTAINED_BY_FALSE_STATEMENT) {
            return reasons.getTenancyByFalseStatementGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGrounds.PREMIUM_PAID_MUTUAL_EXCHANGE) {
            return reasons.getPremiumMutualExchangeGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGrounds.UNREASONABLE_CONDUCT_TIED_ACCOMMODATION) {
            return reasons.getUnreasonableConductGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGrounds.REFUSAL_TO_MOVE_BACK) {
            return reasons.getRefusalToMoveBackGround();
        } else if (ground == SecureAntisocialAdditionalGrounds.S84A_CONDITION_2) {
            return reasons.getAntiSocialCondition2OfS84AGround();
        } else if (ground == SecureAntisocialAdditionalGrounds.S84A_CONDITION_3) {
            return reasons.getAntiSocialCondition3OfS84AGround();
        } else if (ground == SecureAntisocialAdditionalGrounds.S84A_CONDITION_4) {
            return reasons.getAntiSocialCondition4OfS84AGround();
        } else if (ground == SecureAntisocialAdditionalGrounds.S84A_CONDITION_5) {
            return reasons.getAntiSocialCondition5OfS84AGround();
        } else if (ground == SecureOrFlexibleMandatoryGroundsAlternativeAccomm.OVERCROWDING) {
            return reasons.getOvercrowdingGround();
        } else if (ground == SecureOrFlexibleMandatoryGroundsAlternativeAccomm.LANDLORD_WORKS) {
            return reasons.getLandlordWorksGround();
        } else if (ground == SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD) {
            return reasons.getPropertySoldGround();
        } else if (ground == SecureOrFlexibleMandatoryGroundsAlternativeAccomm.CHARITABLE_LANDLORD) {
            return reasons.getCharitableLandlordGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
            .TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE) {
            return reasons.getTiedAccommodationGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ADAPTED_ACCOMMODATION) {
            return reasons.getAdaptedAccommodationGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
            .HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES) {
            return reasons.getHousingAssocSpecialGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.SPECIAL_NEEDS_ACCOMMODATION) {
            return reasons.getSpecialNeedsAccommodationGround();
        } else if (ground == SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.UNDER_OCCUPYING_AFTER_SUCCESSION) {
            return reasons.getUnderOccupancySuccessionGround();
        }

        return null;
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
