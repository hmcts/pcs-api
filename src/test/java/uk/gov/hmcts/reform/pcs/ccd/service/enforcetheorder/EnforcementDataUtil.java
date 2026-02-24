package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

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
                .rawWarrantDetails(RawWarrantDetails.builder().build())
                .warrantDetails(WarrantDetails.builder()
                    .anyRiskToBailiff(YesNoNotSure.YES)
                    .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                            .correctNameAndAddress(VerticalYesNo.YES)
                            .build())
                    .riskCategories(
                            Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE, RiskCategory.VERBAL_OR_WRITTEN_THREATS))
                    .riskDetails(EnforcementRiskDetails.builder()
                            .violentDetails("Violent")
                            .verbalThreatsDetails("Verbal")
                            .build())
                    .build())
                .build();
    }

    static EnforcementOrder buildEnforcementOrderWithVulnerability() {
        return EnforcementOrder.builder()
                .selectEnforcementType(SelectEnforcementType.WARRANT)
                .warrantDetails(WarrantDetails.builder()
                        .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                                .correctNameAndAddress(VerticalYesNo.YES)
                                .build())
                        .build())
                .rawWarrantDetails(RawWarrantDetails.builder()
                        .vulnerablePeoplePresent(YesNoNotSure.YES)
                        .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
                                .vulnerableCategory(VulnerableCategory.VULNERABLE_ADULTS)
                                .vulnerableReasonText("Vulnerability reason")
                                .build())
                        .build())
                .build();
    }

    static EnforcementOrder buildEnforcementOrderWithSelectedDefendants(
        List<DynamicStringListElement> selectedValues,
        List<DynamicStringListElement> listItems) {
        DynamicMultiSelectStringList selectedDefendants = new DynamicMultiSelectStringList(selectedValues, listItems);

        return EnforcementOrder.builder()
            .selectEnforcementType(SelectEnforcementType.WARRANT)
            .warrantDetails(WarrantDetails.builder()
                                .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                                                               .correctNameAndAddress(VerticalYesNo.YES)
                                                               .build())
                                .peopleToEvict(PeopleToEvict.builder()
                                                   .evictEveryone(VerticalYesNo.NO)
                                                   .build())
                                .build())
            .rawWarrantDetails(RawWarrantDetails.builder()
                                   .selectedDefendants(selectedDefendants)
                                   .build())
            .build();
    }
}
