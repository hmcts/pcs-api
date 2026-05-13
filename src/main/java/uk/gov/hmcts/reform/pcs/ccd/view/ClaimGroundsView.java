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
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherNoGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureAntisocialAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class ClaimGroundsView {

    private static final Comparator<ListValue<ClaimGroundSummary>> GROUNDS_COMPARATOR = createGroundsComparator();

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        getMainClaim(pcsCaseEntity)
            .ifPresent(mainClaim -> setClaimGroundFields(pcsCase, mainClaim));
    }

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
                    addGrounds(summaries, selected.getMandatoryGrounds());
                    addGrounds(summaries, selected.getDiscretionaryGrounds());
                });
            }
        } else if (tenancyType == TenancyLicenceType.SECURE_TENANCY
            || tenancyType == TenancyLicenceType.FLEXIBLE_TENANCY) {
            Optional.ofNullable(draftCaseData.getSecureOrFlexiblePossessionGrounds()).ifPresent(selected -> {
                addGrounds(summaries, selected.getSecureOrFlexibleMandatoryGrounds());
                addGrounds(summaries, selected.getSecureOrFlexibleDiscretionaryGrounds());
                addGrounds(summaries, selected.getSecureAntisocialAdditionalGrounds());
                addGrounds(summaries, selected.getSecureOrFlexibleMandatoryGroundsAlt());
                addGrounds(summaries, selected.getSecureOrFlexibleDiscretionaryGroundsAlt());
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
                    .toList());
            }
            if (!CollectionUtils.isEmpty(selected.getAdditionalMandatoryGrounds())) {
                addGrounds(summaries, selected.getAdditionalMandatoryGrounds().stream()
                    .map(ground -> AssuredMandatoryGround.valueOf(ground.name()))
                    .toList());
            }
            if (!CollectionUtils.isEmpty(selected.getAdditionalDiscretionaryGrounds())) {
                addGrounds(summaries, selected.getAdditionalDiscretionaryGrounds().stream()
                    .map(ground -> AssuredDiscretionaryGround.valueOf(ground.name()))
                    .toList());
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
            addGrounds(summaries, Set.of(IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS));
            return;
        }

        addGrounds(summaries, selected.getIntroductoryDemotedOrOtherGrounds());
    }

    private void addGrounds(List<ListValue<ClaimGroundSummary>> summaries,
                            Set<? extends PossessionGroundEnum> grounds) {
        if (!CollectionUtils.isEmpty(grounds)) {
            addGrounds(summaries, List.copyOf(grounds));
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

    private PossessionGroundEnum mapAssuredRentArrearsGround(AssuredRentArrearsGround ground) {
        if (ground == AssuredRentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8) {
            return AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8;
        }

        return AssuredDiscretionaryGround.valueOf(ground.name());
    }

    private void setClaimGroundFields(PCSCase pcsCase,
                                      ClaimEntity mainClaim) {

        Set<ClaimGroundEntity> claimGroundEntities = mainClaim.getClaimGrounds();

        List<ListValue<ClaimGroundSummary>> claimGroundSummaries = claimGroundEntities.stream()
            .map(claimGroundEntity -> {
                ClaimGroundCategory category = claimGroundEntity.getCategory();

                String groundCode = claimGroundEntity.getCode();
                PossessionGroundEnum groundEnum = getGroundEnumValue(category, groundCode);

                ClaimGroundSummary claimGroundSummary = ClaimGroundSummary.builder()
                    .category(category)
                    .code(groundCode)
                    .label(groundEnum.getLabel())
                    .description(claimGroundEntity.getDescription())
                    .reason(claimGroundEntity.getReason())
                    .categoryRank(category.getRank())
                    .groundRank(groundEnum.getRank())
                    .isRentArrears(YesOrNoConverter.toYesOrNo(claimGroundEntity.getIsRentArrears()))
                    .build();

                return ListValue.<ClaimGroundSummary>builder()
                    .id(claimGroundEntity.getId().toString())
                    .value(claimGroundSummary)
                    .build();

            })
            .sorted(GROUNDS_COMPARATOR)
            .toList();

        pcsCase.setClaimGroundSummaries(claimGroundSummaries);

    }

    private static PossessionGroundEnum getGroundEnumValue(ClaimGroundCategory category, String groundCode) {
        return switch (category) {
            case ASSURED_MANDATORY -> AssuredMandatoryGround.valueOf(groundCode);
            case ASSURED_DISCRETIONARY -> AssuredDiscretionaryGround.valueOf(groundCode);
            case SECURE_OR_FLEXIBLE_MANDATORY -> SecureOrFlexibleMandatoryGrounds.valueOf(groundCode);
            case SECURE_OR_FLEXIBLE_ANTISOCIAL -> SecureAntisocialAdditionalGrounds.valueOf(groundCode);
            case SECURE_OR_FLEXIBLE_MANDATORY_ALT
                -> SecureOrFlexibleMandatoryGroundsAlternativeAccomm.valueOf(groundCode);
            case SECURE_OR_FLEXIBLE_DISCRETIONARY -> SecureOrFlexibleDiscretionaryGrounds.valueOf(groundCode);
            case SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT
                -> SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.valueOf(groundCode);
            case INTRODUCTORY_DEMOTED_OTHER -> IntroductoryDemotedOrOtherGrounds.valueOf(groundCode);
            case INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS -> IntroductoryDemotedOrOtherNoGrounds.valueOf(groundCode);
            case WALES_STANDARD_OTHER_MANDATORY -> MandatoryGroundWales.valueOf(groundCode);
            case WALES_STANDARD_OTHER_DISCRETIONARY -> DiscretionaryGroundWales.valueOf(groundCode);
            case WALES_SECURE_MANDATORY -> SecureContractMandatoryGroundsWales.valueOf(groundCode);
            case WALES_SECURE_DISCRETIONARY -> SecureContractDiscretionaryGroundsWales.valueOf(groundCode);
            case WALES_STANDARD_OTHER_ESTATE_MANAGEMENT, WALES_SECURE_ESTATE_MANAGEMENT
                -> EstateManagementGroundsWales.valueOf(groundCode);
        };
    }

    private static Optional<ClaimEntity> getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst();
    }

    private static Comparator<ListValue<ClaimGroundSummary>> createGroundsComparator() {
        return Comparator.<ListValue<ClaimGroundSummary>, Integer>comparing(g -> g.getValue().getCategoryRank())
            .thenComparing(g -> g.getValue().getGroundRank());
    }

}
