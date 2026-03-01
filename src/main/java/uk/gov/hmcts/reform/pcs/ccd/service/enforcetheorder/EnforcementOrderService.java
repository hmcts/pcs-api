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
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementWarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrantofrestitution.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.writofrestitution.WritOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementRiskProfileRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementSelectedDefendantRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant.EnforcementWarrantRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrantofrestitution.WarrantOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.writofrestitution.WritOfRestitutionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.EnforcementRiskProfileMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.EnforcementWarrantMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.SelectedDefendantsMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;

import java.util.List;

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
    private final EnforcementWarrantMapper enforcementWarrantMapper;
    private final EnforcementWarrantRepository enforcementWarrantRepository;
    private final WritOfRestitutionRepository writOfRestitutionRepository;

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

        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcementOrderEntity);

        createSelectedDefendants(saved);

        if (SelectEnforcementType.WARRANT == enforcementOrder.getSelectEnforcementType()
            && enforcementOrder.getWarrantDetails() != null) {
            EnforcementRiskProfileEntity riskProfile =
                enforcementRiskProfileMapper.toEntity(saved, enforcementOrder);
            enforcementRiskProfileRepository.save(riskProfile);
            storeWarrant(enforcementOrder, saved);
        } else if (enforcementOrder.getSelectEnforcementType() == SelectEnforcementType.WARRANT_OF_RESTITUTION) {
            createWarrantOfRestitution(saved);
        } else if (enforcementOrder.getSelectEnforcementType() == SelectEnforcementType.WRIT_OF_RESTITUTION) {
            createWritOfRestitutionEntity(saved);
        }
    }

    private void createSelectedDefendants(EnforcementOrderEntity enforcementOrderEntity) {
        List<EnforcementSelectedDefendantEntity> selectedDefendantsEntities =
                selectedDefendantsMapper.mapToEntities(enforcementOrderEntity);

        if (!CollectionUtils.isEmpty(selectedDefendantsEntities)) {
            enforcementSelectedDefendantRepository.saveAll(selectedDefendantsEntities);
        }
    }

    private void storeWarrant(EnforcementOrder enforcementOrder, EnforcementOrderEntity enforcementOrderEntity) {
        EnforcementWarrantEntity warrantEntity = enforcementWarrantMapper.toEntity(enforcementOrder,
                                                                                   enforcementOrderEntity);
        EnforcementWarrantEntity saved = enforcementWarrantRepository.save(warrantEntity);
        enforcementOrderEntity.setWarrantDetails(saved);
    }

    private void createWarrantOfRestitution(EnforcementOrderEntity enforcementOrderEntity) {
        WarrantOfRestitutionEntity warrantOfRestitutionEntity = new WarrantOfRestitutionEntity();
        warrantOfRestitutionEntity.setEnforcementOrder(enforcementOrderEntity);
        warrantOfRestitutionRepository.save(warrantOfRestitutionEntity);
    }

    private void createWritOfRestitutionEntity(EnforcementOrderEntity enforcementOrderEntity) {
        WritOfRestitutionEntity writOfRestitutionEntity = new WritOfRestitutionEntity();
        writOfRestitutionEntity.setEnforcementOrder(enforcementOrderEntity);
        writOfRestitutionRepository.save(writOfRestitutionEntity);
    }
}
