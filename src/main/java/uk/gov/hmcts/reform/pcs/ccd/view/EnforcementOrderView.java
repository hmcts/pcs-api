package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderStepRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Component
@RequiredArgsConstructor
public class EnforcementOrderView {

    private final DraftCaseDataService draftCaseDataService;
    private final OrderStepRepository orderStepRepository;

    public void setCaseFields(PCSCase pcsCase, long caseReference, UUID caseId, State state) {
        if (State.CASE_ISSUED == state) {
            setHasEnforcementDraftData(caseReference, pcsCase);
            setIsSuspendedOrder(caseId, pcsCase);
        }
    }

    private void setHasEnforcementDraftData(long caseReference, PCSCase pcsCase) {
        boolean hasUnsubmittedCaseData = draftCaseDataService
            .hasUnsubmittedCaseData(caseReference, enforceTheOrder);
        if (hasUnsubmittedCaseData) {
            Optional<PCSCase> unsubmittedCaseData = draftCaseDataService
                .getUnsubmittedCaseData(caseReference, enforceTheOrder);
            unsubmittedCaseData.ifPresent(inflated ->
                                              pcsCase.setEnforcementOrder(inflated.getEnforcementOrder()));
            pcsCase.setHasUnsubmittedEnforcementData(YesOrNo.YES);
        } else {
            pcsCase.setHasUnsubmittedEnforcementData(YesOrNo.NO);
        }
    }

    /**
     * Enforcement follows the case's most recently issued order: a suspended possession order takes the suspended
     * journey, and any other order, or none, the not suspended one. It is worked out each time the case is read, so a
     * draft resumed after a later order is issued follows that order.
     */
    private void setIsSuspendedOrder(UUID caseId, PCSCase pcsCase) {
        boolean suspended = latestIssuedOrderType(caseId)
            .filter(OrderType.SUSPENDED_POSSESSION::equals)
            .isPresent();
        pcsCase.setEnforcementIsSuspendedOrder(YesOrNo.from(suspended));
    }

    private Optional<OrderType> latestIssuedOrderType(UUID caseId) {
        return orderStepRepository.findLatestIssuedByCaseId(caseId).map(OrderStepEntity::getOrderType);
    }
}
