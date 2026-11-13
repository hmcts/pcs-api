package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategyFactory;

import java.util.Set;

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

    @Transactional
    public void updateStatesForBreathingSpace(long caseReference) {
        // Discard all unsubmitted Enforce the Order drafts for this case.
        int discardedDrafts =
            draftCaseDataService.discardAllUnsubmittedEventData(
                caseReference,
                EventId.enforceTheOrder
            );

        log.info(
            "Discarded {} enforcement drafts for case {}",
            discardedDrafts,
            caseReference
        );

        // Preserve AC02: transition live enforcement applications.
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);

        Set<EnforcementOrderEntity> applications =
            pcsCase.getEnforcementOrders();

        if (CollectionUtils.isEmpty(applications)) {
            log.info(
                "No enforcement applications found for case {}",
                caseReference
            );
            return;
        }

        for (EnforcementOrderEntity application : applications) {
            if (application.getState() == EnforcementState.CASE_PROGRESSION) {
                application.setState(EnforcementState.BREATHING_SPACE);
            }
        }
    }

    private EnforcementOrderEntity createEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder,
                                                          EnforcementState state) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        EnforcementOrderEntity enforcementOrderEntity = mapToEntity(enforcementOrder, pcsCaseEntity, state);
        strategyFactory.getStrategy(SelectEnforcementType.getSelectEnforcementTypeFromName(
                        enforcementOrder.getChooseEnforcementType().getValueCode()))
                .process(enforcementOrderEntity, enforcementOrder);

        return enforcementOrderRepository.save(enforcementOrderEntity);
    }

    private EnforcementOrderEntity mapToEntity(EnforcementOrder enforcementOrder, PcsCaseEntity pcsCaseEntity,
                                               EnforcementState state) {
        EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
        enforcementOrderEntity.setPcsCase(pcsCaseEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrder);
        enforcementOrderEntity.setState(state);
        return enforcementOrderEntity;
    }
}
