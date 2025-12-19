package uk.gov.hmcts.reform.pcs.ccd.service.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcement.EnforcementOrderEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

final class EnforcementDataUtil {

    static PcsCaseEntity buildPcsCaseEntity(UUID pcsId, UUID claimId) {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setId(pcsId);
        pcsCaseEntity.setCaseReference(1234L);
        pcsCaseEntity.setClaims(List.of(buildClaimEntity(claimId, pcsCaseEntity)));
        return pcsCaseEntity;
    }

    private static ClaimEntity buildClaimEntity(UUID claimId, PcsCaseEntity pcsCase) {
        ClaimEntity claimEntity = new ClaimEntity();
        claimEntity.setId(claimId);
        claimEntity.setPcsCase(pcsCase);
        return claimEntity;
    }

    static EnforcementOrderEntity buildEnforcementOrderEntity(UUID enfId, ClaimEntity claimEntity,
            EnforcementOrder enforcementOrder) {
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setId(enfId);
        enforcementOrderEntity.setClaim(claimEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);
        return enforcementOrderEntity;
    }

    static EnforcementOrder buildEnforcementOrder() {
        return EnforcementOrder.builder()
                .selectEnforcementType(SelectEnforcementType.WARRANT)
                .anyRiskToBailiff(YesNoNotSure.YES)
                .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                        .correctNameAndAddress(VerticalYesNo.YES)
                        .build())
                .enforcementRiskCategories(
                        Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE, RiskCategory.VERBAL_OR_WRITTEN_THREATS))
                .riskDetails(EnforcementRiskDetails.builder()
                        .enforcementViolentDetails("Violent")
                        .enforcementVerbalOrWrittenThreatsDetails("Verbal")
                        .build())
                .build();
    }
}
