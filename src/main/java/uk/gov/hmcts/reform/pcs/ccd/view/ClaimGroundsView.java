package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherNoGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class ClaimGroundsView {

    private static final Comparator<ListValue<ClaimGroundSummary>> GROUNDS_COMPARATOR = createGroundsComparator();

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        // TODO: Wales to be handled under HDPI-3824
        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            return;
        }

        getMainClaim(pcsCaseEntity)
            .ifPresent(mainClaim -> setClaimGroundFields(pcsCase, mainClaim));
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
            case SECURE_OR_FLEXIBLE_MANDATORY_ALT
                -> SecureOrFlexibleMandatoryGroundsAlternativeAccomm.valueOf(groundCode);
            case SECURE_OR_FLEXIBLE_DISCRETIONARY -> SecureOrFlexibleDiscretionaryGrounds.valueOf(groundCode);
            case SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT
                -> SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.valueOf(groundCode);
            case INTRODUCTORY_DEMOTED_OTHER -> IntroductoryDemotedOrOtherGrounds.valueOf(groundCode);
            case INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS -> IntroductoryDemotedOrOtherNoGrounds.valueOf(groundCode);
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
