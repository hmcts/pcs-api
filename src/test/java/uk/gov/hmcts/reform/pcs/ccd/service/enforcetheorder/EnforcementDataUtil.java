package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory.VULNERABLE_CHILDREN;

public final class EnforcementDataUtil {

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

    public static EnforcementOrder buildEnforcementOrder() {
        return EnforcementOrder.builder()
                .chooseEnforcementType(buildEnforcementTypes(WARRANT))
                .rawWarrantDetails(RawWarrantDetails.builder().build())
                .warrantDetails(WarrantDetails.builder()
                    .anyRiskToBailiff(YesNoNotSure.YES)
                    .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                            .correctNameAndAddress(VerticalYesNo.YES)
                            .build())
                    .riskCategories(
                            Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE, RiskCategory.VERBAL_OR_WRITTEN_THREATS))
                    .riskDetails(RiskDetails.builder()
                            .violentDetails("Violent")
                            .verbalThreatsDetails("Verbal")
                            .build())
                    .build())
                .build();
    }

    public static EnforcementOrder buildEnforcementOrderWithVulnerability() {
        return EnforcementOrder.builder()
                .chooseEnforcementType(buildEnforcementTypes(WARRANT))
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

    public static EnforcementOrder buildEnforcementOrderWithSelectedDefendants(
        List<DynamicStringListElement> selectedValues,
        List<DynamicStringListElement> listItems) {
        DynamicMultiSelectStringList selectedDefendants = new DynamicMultiSelectStringList(selectedValues, listItems);

        return EnforcementOrder.builder()
            .chooseEnforcementType(buildEnforcementTypes(WARRANT))
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

    public static EnforcementOrder buildEnforcementOrderWithSpecifiedType(SelectEnforcementType enforcementType) {
        return EnforcementOrder.builder()
                .chooseEnforcementType(buildEnforcementTypes(enforcementType))
                .warrantDetails(WarrantDetails.builder()
                        .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                                .correctNameAndAddress(VerticalYesNo.YES)
                                .build())
                        .build())
                .rawWarrantDetails(RawWarrantDetails.builder().build())
                .build();
    }

    public static DynamicStringList buildEnforcementTypes(SelectEnforcementType enforcementType) {
        DynamicStringList allTypes = buildEnforcementTypes();

        return DynamicStringList.builder()
                .value(DynamicStringListElement.builder()
                        .code(enforcementType.name())
                        .label(enforcementType.getLabel())
                        .build())
                .listItems(allTypes.getListItems())
                .build();
    }

    public static DynamicStringList buildEnforcementTypes() {
        List<DynamicStringListElement> listItems = Arrays.stream(SelectEnforcementType.values())
                .map(type -> DynamicStringListElement.builder()
                        .code(type.name())
                        .label(type.getLabel())
                        .build())
                .toList();

        return DynamicStringList.builder()
                .listItems(listItems)
                .build();
    }

    public static RiskProfileEntity createExpectedRiskProfileEntity(EnforcementOrderEntity enforcementOrderEntity) {
        RiskProfileEntity entity = new RiskProfileEntity();
        entity.setEnforcementOrder(enforcementOrderEntity);
        entity.setAnyRiskToBailiff(YesNoNotSure.YES);
        entity.setViolentDetails("Violent behavior reported");
        entity.setFirearmsDetails("Firearms present");
        entity.setCriminalDetails("Criminal history");
        entity.setVerbalThreatsDetails("Verbal threats made");
        entity.setProtestGroupDetails("Member of protest group");
        entity.setPoliceSocialServicesDetails("Police involvement");
        entity.setAnimalsDetails("Aggressive dogs on premises");
        entity.setVulnerablePeoplePresent(YesNoNotSure.YES);
        entity.setVulnerableCategory(VULNERABLE_CHILDREN);
        entity.setVulnerableReasonText("Young children present");
        return entity;
    }
}
