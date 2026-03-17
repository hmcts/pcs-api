package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.ConfirmEvictionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.ConfirmEvictionRepository;
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
    private final ConfirmEvictionRepository confirmEvictionRepository;

    @Transactional
    public void saveAndClearDraftData(long caseReference, EnforcementOrder enforcementOrder) {
        createEnforcementOrder(caseReference, enforcementOrder);
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, EventId.enforceTheOrder);
    }

    private void createEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder) {
        ClaimEntity claimEntity = getClaimEntity(caseReference);
        EnforcementOrderEntity orderEntity = enforcementOrderRepository
            .save(mapToEntity(enforcementOrder, claimEntity));
        strategyFactory.getStrategy(enforcementOrder.getSelectEnforcementType())
            .process(orderEntity, enforcementOrder);
    }

    private ClaimEntity getClaimEntity(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
        List<ClaimEntity> claimEntities = pcsCaseEntity.getClaims();
        if (CollectionUtils.isEmpty(claimEntities)) {
            log.error("No claim found for case reference {}", caseReference);
            throw new ClaimNotFoundException(pcsCaseEntity.getCaseReference());
        }
        // Assuming 1 claim per PcsCase
        return claimEntities.getFirst();
    }

    public void confirmEviction(long caseReference, EnforcementOrder enforcementOrder) {
        ConfirmEvictionEntity confirmEviction = mapToConfirmEvictionEntity(caseReference, enforcementOrder);
        confirmEvictionRepository.save(confirmEviction);
    }

    private ConfirmEvictionEntity mapToConfirmEvictionEntity(long caseReference, EnforcementOrder enforcementOrder) {
        EnforcementOrderEntity orderEntity = enforcementOrderRepository
            .save(mapToEntity(enforcementOrder, getClaimEntity(caseReference)));
        ConfirmEvictionEntity confirmEviction = new ConfirmEvictionEntity();
        confirmEviction.setEnforcementOrder(orderEntity);

        // ...

        return confirmEviction;
    }

    private EnforcementOrderEntity mapToEntity(EnforcementOrder enforcementOrder, ClaimEntity claimEntity) {
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(claimEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);
        return enforcementOrderEntity;
    }

}
