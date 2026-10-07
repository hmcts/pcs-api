package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;

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

    private void setClaimGroundFields(PCSCase pcsCase,
                                      ClaimEntity mainClaim) {

        Set<ClaimGroundEntity> claimGroundEntities = mainClaim.getClaimGrounds();

        List<ListValue<ClaimGroundSummary>> claimGroundSummaries = claimGroundEntities.stream()
            .map(claimGroundEntity -> {
                ClaimGroundCategory category = claimGroundEntity.getCategory();

                String groundCode = claimGroundEntity.getCode();
                PossessionGroundEnum groundEnum = ClaimGroundSummary.resolveGround(category, groundCode);

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

    private static Optional<ClaimEntity> getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst();
    }

    private static Comparator<ListValue<ClaimGroundSummary>> createGroundsComparator() {
        return Comparator.<ListValue<ClaimGroundSummary>, Integer>comparing(g -> g.getValue().getCategoryRank())
            .thenComparing(g -> g.getValue().getGroundRank());
    }

}
