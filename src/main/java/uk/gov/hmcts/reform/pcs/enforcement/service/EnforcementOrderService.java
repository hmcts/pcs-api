package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.service.strategy.EnforcementTypeStrategyFactory;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final PcsCaseService pcsCaseService;
    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementTypeStrategyFactory strategyFactory;

    public EnforcementOrder retrieveEnforcementOrder(long caseReference, SelectEnforcementType enforcementType) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseEntity);
        List<EnforcementOrderEntity> enforcementEntities =
                enforcementOrderRepository.findByClaimId(claimEntity.getId());

        return enforcementEntities
                .stream()
                .map(EnforcementOrderEntity::getEnforcementOrder)
                .filter(order ->
                        order.getChooseEnforcementType().getValue().getCode().equals(enforcementType.name()))
                .findFirst()
                .orElse(null);
    }

    ClaimEntity retrieveClaimEntity(PcsCaseEntity pcsCaseEntity) {
        List<ClaimEntity> claimEntities = pcsCaseEntity.getClaims();

        // Assuming 1 claim per PcsCase
        return claimEntities.getFirst();
    }

    @Transactional
    public void saveAndClearDraftData(long caseReference, EnforcementOrder enforcementOrder) {
        createEnforcementOrder(caseReference, enforcementOrder);
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, EventId.enforceTheOrder);
    }

    private void createEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        ClaimEntity claimEntity = retrieveClaimEntity(pcsCaseEntity);

        EnforcementOrderEntity enforcementOrderEntity = mapToEntity(enforcementOrder, claimEntity);
        strategyFactory.getStrategy(SelectEnforcementType.getSelectEnforcementTypeFromName(
                        enforcementOrder.getChooseEnforcementType().getValueCode()))
                .process(enforcementOrderEntity, enforcementOrder);
        enforcementOrderRepository.save(enforcementOrderEntity);
    }

    private EnforcementOrderEntity mapToEntity(EnforcementOrder enforcementOrder, ClaimEntity claimEntity) {
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setClaim(claimEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);
        return enforcementOrderEntity;
    }
}
