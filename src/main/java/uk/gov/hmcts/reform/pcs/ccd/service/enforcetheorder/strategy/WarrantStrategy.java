package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.RiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.SelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.SelectedDefendantsMapper;

import java.util.List;

@Component
@AllArgsConstructor
public class WarrantStrategy implements EnforcementTypeStrategy {

    private final RiskProfileRepository riskProfileRepository;
    private final SelectedDefendantsMapper selectedDefendantsMapper;
    private final SelectedDefendantRepository selectedDefendantRepository;

    @Override
    public void process(EnforcementOrderEntity enforcementOrderEntity, EnforcementOrder enforcementOrder) {
        RiskProfileEntity riskProfile = mapToRiskProfile(enforcementOrderEntity, enforcementOrder);
        riskProfileRepository.save(riskProfile);
    }

    private RiskProfileEntity mapToRiskProfile(EnforcementOrderEntity enforcementOrderEntity,
        EnforcementOrder enforcementOrder) {
        RiskProfileEntity entity = new RiskProfileEntity();
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

        List<SelectedDefendantEntity> selectedDefendantsEntities =
            selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);
        if (!CollectionUtils.isEmpty(selectedDefendantsEntities)) {
            selectedDefendantRepository.saveAll(selectedDefendantsEntities);
        }
        return entity;
    }

}
