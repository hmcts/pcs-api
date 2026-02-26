package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategyFactory;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final PcsCaseRepository pcsCaseRepository;
    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementTypeStrategyFactory strategyFactory;

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
        EnforcementOrderEntity orderEntity = enforcementOrderRepository
            .save(mapToEntity(enforcementOrder, claimEntity));
        strategyFactory.getStrategy(enforcementOrder.getSelectEnforcementType())
            .process(orderEntity, enforcementOrder);
    }

    private void storeWarrant(EnforcementOrder enforcementOrder, EnforcementOrderEntity enforcementOrderEntity) {
        EnforcementWarrantEntity warrantEntity = enforcementWarrantMapper.toEntity(enforcementOrder,
                                                                                   enforcementOrderEntity);
        EnforcementWarrantEntity saved = enforcementWarrantRepository.save(warrantEntity);
        enforcementOrderEntity.setWarrantDetails(saved);
    }

    private void createWritOfRestitutionEntity(EnforcementOrderEntity enforcementOrderEntity) {
        WritOfRestitutionEntity writOfRestitutionEntity = new WritOfRestitutionEntity();
        writOfRestitutionEntity.setEnforcementOrder(enforcementOrderEntity);
        writOfRestitutionRepository.save(writOfRestitutionEntity);
    }

    private EnforcementOrderEntity mapToEntity(EnforcementOrder enforcementOrder, ClaimEntity claimEntity) {
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(claimEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);
        return enforcementOrderEntity;
    }

}
