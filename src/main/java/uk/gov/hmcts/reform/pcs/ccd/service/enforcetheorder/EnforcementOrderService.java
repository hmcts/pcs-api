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
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy.EnforcementTypeStrategyFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.PENDING_CASE_ISSUED;

@Service
@Slf4j
@AllArgsConstructor
public class EnforcementOrderService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final PcsCaseService pcsCaseService;
    private final DraftCaseDataService draftCaseDataService;
    private final EnforcementTypeStrategyFactory strategyFactory;

    @Transactional
    public void createEnforcementOrder(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        if (CollectionUtils.isEmpty(pcsCaseEntity.getEnforcementOrders())
                || pcsCaseEntity.getEnforcementOrders().stream()
                    .noneMatch(order -> order.getState() == AWAITING_SUBMISSION_TO_HMCTS)) {
            EnforcementOrderEntity enforcementOrderEntity = EnforcementOrderEntity.builder()
                    .pcsCase(pcsCaseEntity)
                    .enforcementOrder(EnforcementOrder.builder().build())
                    .state(AWAITING_SUBMISSION_TO_HMCTS)
                    .build();
            pcsCaseEntity.getEnforcementOrders().add(enforcementOrderEntity);
            enforcementOrderRepository.save(enforcementOrderEntity);
        }
    }

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

        // Preserve AC02
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);

        Set<EnforcementOrderEntity> applications =
            pcsCase.getEnforcementOrders();

        if (CollectionUtils.isEmpty(applications)) {
            log.info("No enforcement state found for case {}", caseReference);
            return;
        }

        for (EnforcementOrderEntity application : applications) {
            EnforcementState currentState = application.getState();

            if (currentState == EnforcementState.CASE_PROGRESSION) {
                application.setState(EnforcementState.BREATHING_SPACE);
                log.info("Updated enforcement state to BREATHING_SPACE for case {}", caseReference);

            } else if (currentState == EnforcementState.AWAITING_SUBMISSION_TO_HMCTS
                || currentState == EnforcementState.PENDING_CASE_ISSUED) {

                application.setState(EnforcementState.DRAFT_DISCARDED);
                log.info("Updated enforcement state to Draft Discarded for case {}", caseReference);
            }
        }
    }

    @Transactional
    public EnforcementOrderEntity submitEnforcementOrder(long caseReference, EnforcementOrder enforcementOrder) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        EnforcementOrderEntity enfEntity = findEnforcementOrderEntity(pcsCaseEntity);
        enfEntity.setEnforcementOrder(enforcementOrder);
        enfEntity.setState(PENDING_CASE_ISSUED);
        strategyFactory.getStrategy(SelectEnforcementType.valueOf(
                        enforcementOrder.getChooseEnforcementType().getValueCode()))
                .process(enfEntity, enforcementOrder);

        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, EventId.enforceTheOrder);

        return enforcementOrderRepository.save(enfEntity);
    }

    private EnforcementOrderEntity findEnforcementOrderEntity(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getEnforcementOrders().stream()
                .filter(order -> order.getState() == AWAITING_SUBMISSION_TO_HMCTS)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No enforcement order found for case reference " + pcsCaseEntity.getCaseReference()
                                + " with state " + AWAITING_SUBMISSION_TO_HMCTS));
    }
}
