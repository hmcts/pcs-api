package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategyFactory;

import java.util.Set;

@Service
@AllArgsConstructor
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final PcsCaseService pcsCaseService;
    private final PcsCaseRepository pcsCaseRepository;
    private final EnforcementStateService enforcementStateService;
    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementTypeStrategyFactory strategyFactory;

    public EnforcementOrder retrieveEnforcementOrder(long caseReference, SelectEnforcementType enforcementType) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        Set<EnforcementOrderEntity> enforcementEntitySet = pcsCaseEntity.getEnforcementOrders();
        if (CollectionUtils.isEmpty(enforcementEntitySet)) {
            return null;
        }

        return enforcementEntitySet
                .stream()
                .map(EnforcementOrderEntity::getEnforcementOrder)
                .filter(order ->
                        order.getChooseEnforcementType().getValue().getCode().equals(enforcementType.name()))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public EnforcementOrderEntity saveAndClearDraftData(long caseReference, EnforcementOrder enforcementOrder,
                                                        EnforcementState state) {
        EnforcementOrderEntity enforcementOrderEntity = createEnforcementOrder(caseReference, enforcementOrder, state);
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, EventId.enforceTheOrder);

        return enforcementOrderEntity;
    }

    private EnforcementOrderEntity createEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder,
                                                          EnforcementState state) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        EnforcementOrderEntity enforcementOrderEntity = mapToEntity(enforcementOrder, pcsCaseEntity);
        strategyFactory.getStrategy(SelectEnforcementType.getSelectEnforcementTypeFromName(
                        enforcementOrder.getChooseEnforcementType().getValueCode()))
                .process(enforcementOrderEntity, enforcementOrder);
        enforcementOrderEntity.setRank(nextRankForType(pcsCaseEntity, enforcementOrderEntity));

        enforcementStateService.mapToEntity(pcsCaseEntity, state);
        pcsCaseRepository.save(pcsCaseEntity);
        return enforcementOrderRepository.save(enforcementOrderEntity);
    }

    /**
     * One more than the number of the case's existing applications of the same type, so writs and warrants are
     * numbered separately.
     */
    private static int nextRankForType(PcsCaseEntity pcsCaseEntity, EnforcementOrderEntity newApplication) {
        long sameType = pcsCaseEntity.getEnforcementOrders().stream()
            .filter(existing -> existing != newApplication)
            .filter(existing -> (existing.getWritDetails() != null) == (newApplication.getWritDetails() != null)
                && (existing.getWarrantDetails() != null) == (newApplication.getWarrantDetails() != null)
                && (existing.getWarrantOfRestitutionDetails() != null)
                    == (newApplication.getWarrantOfRestitutionDetails() != null))
            .count();
        return (int) sameType + 1;
    }

    private EnforcementOrderEntity mapToEntity(EnforcementOrder enforcementOrder, PcsCaseEntity pcsCaseEntity) {
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setPcsCase(pcsCaseEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);
        return enforcementOrderEntity;
    }
}
