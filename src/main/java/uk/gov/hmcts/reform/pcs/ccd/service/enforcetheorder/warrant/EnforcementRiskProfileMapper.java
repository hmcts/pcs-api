package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementRiskProfileEntity;

/**
 * Maps domain {@link EnforcementOrder} (warrant + raw warrant risk details) to {@link EnforcementRiskProfileEntity}.
 */
@Component
public class EnforcementRiskProfileMapper {

    /**
     * Maps enforcement order and its persisted entity to a risk profile entity.
     *
     * @param enforcementOrderEntity the persisted enforcement order entity
     * @param enforcementOrder the domain enforcement order with warrant and raw warrant details
     * @return populated risk profile entity
     */
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
