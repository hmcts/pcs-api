package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementSelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementRiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementRiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementSelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.EnforcementOrderNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final EnforcementRiskProfileRepository enforcementRiskProfileRepository;
    private final PcsCaseRepository pcsCaseRepository;
    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementSelectedDefendantRepository enforcementSelectedDefendantRepository;
    private final SelectedDefendantsMapper selectedDefendantsMapper;

    public EnforcementOrderEntity loadEnforcementOrder(UUID id) {
        return enforcementOrderRepository.findById(id)
                .orElseThrow(() -> new EnforcementOrderNotFoundException(id));
    }

    @Transactional
    public void saveAndClearDraftData(long caseReference, EnforcementOrder enforcementOrder) {
        createEnforcementOrder(caseReference, enforcementOrder);
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, EventId.enforceTheOrder);
    }

    private void createEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder) {
        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
                .orElseThrow(() -> new CaseNotFoundException(caseReference));

        List<ClaimEntity> claimEntities = pcsCaseEntity.getClaims();
        // This should never happen
        if (CollectionUtils.isEmpty(claimEntities)) {
            log.error("No claim found for case reference {}", caseReference);
            throw new ClaimNotFoundException(pcsCaseEntity.getCaseReference());
        }

        // Assuming 1 claim per PcsCase
        ClaimEntity claimEntity = claimEntities.getFirst();

        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(claimEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);

        enforcementOrderRepository.save(enforcementOrderEntity);

        if (enforcementOrder.getSelectEnforcementType() == SelectEnforcementType.WARRANT) {
            EnforcementRiskProfileEntity riskProfile = mapToRiskProfile(enforcementOrderEntity, enforcementOrder);
            enforcementRiskProfileRepository.save(riskProfile);
        }

        List<EnforcementSelectedDefendantEntity> selectedDefendantsEntities =
            selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);

        if (!CollectionUtils.isEmpty(selectedDefendantsEntities)) {
            enforcementSelectedDefendantRepository.saveAll(selectedDefendantsEntities);
        }
    }

    private EnforcementRiskProfileEntity mapToRiskProfile(
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
