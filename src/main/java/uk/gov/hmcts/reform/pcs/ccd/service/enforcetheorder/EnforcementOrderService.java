package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementSelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementRiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrantofrestitution.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementRiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementSelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrantofrestitution.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.EnforcementRiskProfileMapper;
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
    private final WarrantOfRestitutionRepository warrantOfRestitutionRepository;
    private final EnforcementRiskProfileMapper enforcementRiskProfileMapper;
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

        createSelectedDefendants(enforcementOrderEntity);

        if (enforcementOrder.getSelectEnforcementType() == SelectEnforcementType.WARRANT) {
            EnforcementRiskProfileEntity riskProfile =
                    enforcementRiskProfileMapper.toEntity(enforcementOrderEntity, enforcementOrder);
            enforcementRiskProfileRepository.save(riskProfile);
        } else if (enforcementOrder.getSelectEnforcementType() == SelectEnforcementType.WARRANT_OF_RESTITUTION) {
            createWarrantOfRestitution(enforcementOrderEntity);
        }
    }

    private void createSelectedDefendants(EnforcementOrderEntity enforcementOrderEntity) {
        List<EnforcementSelectedDefendantEntity> selectedDefendantsEntities =
                selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);

        if (!CollectionUtils.isEmpty(selectedDefendantsEntities)) {
            enforcementSelectedDefendantRepository.saveAll(selectedDefendantsEntities);
        }
    }

    private void createWarrantOfRestitution(EnforcementOrderEntity enforcementOrderEntity) {
        WarrantOfRestitutionEntity warrantOfRestitutionEntity = new WarrantOfRestitutionEntity();
        warrantOfRestitutionEntity.setEnforcementOrder(enforcementOrderEntity);
        warrantOfRestitutionRepository.save(warrantOfRestitutionEntity);
    }
}
