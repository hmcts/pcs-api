package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementRiskProfileEntity;

@Component
public class EnforcementRiskProfileMapper {

    public EnforcementRiskProfileEntity toEntity(
            EnforcementOrderEntity enforcementOrderEntity,
            EnforcementOrder enforcementOrder) {
        EnforcementRiskProfileEntity entity = new EnforcementRiskProfileEntity();
        entity.setEnforcementOrder(enforcementOrderEntity);

        WarrantDetails warrantDetails = enforcementOrder.getWarrantDetails();
        if (warrantDetails != null) {
            entity.setAnyRiskToBailiff(warrantDetails.getAnyRiskToBailiff());
            EnforcementRiskDetails riskDetails = warrantDetails.getRiskDetails();
            if (riskDetails != null) {
                entity.setViolentDetails(riskDetails.getEnforcementViolentDetails());
                entity.setFirearmsDetails(riskDetails.getEnforcementFirearmsDetails());
                entity.setCriminalDetails(riskDetails.getEnforcementCriminalDetails());
                entity.setVerbalThreatsDetails(riskDetails.getEnforcementVerbalOrWrittenThreatsDetails());
                entity.setProtestGroupDetails(riskDetails.getEnforcementProtestGroupMemberDetails());
                entity.setPoliceSocialServicesDetails(
                        riskDetails.getEnforcementPoliceOrSocialServicesDetails());
                entity.setAnimalsDetails(riskDetails.getEnforcementDogsOrOtherAnimalsDetails());
            }
        }

        RawWarrantDetails rawWarrantDetails = enforcementOrder.getRawWarrantDetails();
        if (rawWarrantDetails != null) {
            entity.setVulnerablePeoplePresent(rawWarrantDetails.getVulnerablePeoplePresent());
            if (rawWarrantDetails.getVulnerableAdultsChildren() != null) {
                entity.setVulnerableCategory(
                        rawWarrantDetails.getVulnerableAdultsChildren().getVulnerableCategory());
                entity.setVulnerableReasonText(
                        rawWarrantDetails.getVulnerableAdultsChildren().getVulnerableReasonText());
            }
        }

        return entity;
    }
}
