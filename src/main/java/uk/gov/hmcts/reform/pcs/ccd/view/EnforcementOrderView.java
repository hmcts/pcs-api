package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementStateEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Component
@RequiredArgsConstructor
public class EnforcementOrderView {

    private final DraftCaseDataService draftCaseDataService;

    public void setCaseFields(PCSCase pcsCase, long caseReference, State state, PcsCaseEntity pcsCaseEntity) {
        setHasEnforcementDraftData(caseReference, state, pcsCase);
        setActiveEnforcementState(pcsCaseEntity, pcsCase);
    }

    private void setHasEnforcementDraftData(long caseReference, State state, PCSCase pcsCase) {
        if (State.CASE_ISSUED == state) {
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
    }

    private void setActiveEnforcementState(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        EnforcementStateEntity enforcementStateEntity = pcsCaseEntity.getEnforcementState();

        if (enforcementStateEntity == null) {
            return;
        }

        pcsCase.setActiveEnforcementState(enforcementStateEntity.getState());
    }
}
