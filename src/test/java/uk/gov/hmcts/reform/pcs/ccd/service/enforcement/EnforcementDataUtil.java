package uk.gov.hmcts.reform.pcs.ccd.service.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcement.EnforcementDataEntity;

import java.util.Set;
import java.util.UUID;

final class EnforcementDataUtil {

    static EnforcementOrder buildSampleEnforcementData() {
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

    static EnforcementDataEntity buildSampleEnforcementDataEntity(UUID enfId, UUID pcsId) {
        EnforcementDataEntity enforcementDataEntity = new EnforcementDataEntity();
        enforcementDataEntity.setId(enfId);
        enforcementDataEntity.setPcsCase(buildPcsCaseEntity(pcsId));
        enforcementDataEntity.setEnforcementData("{"
                + "\"selectEnforcementType\":\"WARRANT\","
                + "\"correctNameAndAddress\":\"YES\","
                + "\"anyRiskToBailiff\":\"YES\","
                + "\"enforcementRiskCategories\":[\"VERBAL_OR_WRITTEN_THREATS\",\"VIOLENT_OR_AGGRESSIVE\"],"
                + "\"enforcementViolentDetails\":\"Violent\","
                + "\"enforcementVerbalOrWrittenThreatsDetails\":\"Verbal\""
                + "}");
        return enforcementDataEntity;
    }

    static PcsCaseEntity buildPcsCaseEntity(UUID pcsId) {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setId(pcsId);
        pcsCaseEntity.setCaseReference(1234L);
        return pcsCaseEntity;
    }
}
