package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Component
@RequiredArgsConstructor
public class EnforcementOrderView {

    private final DraftCaseDataService draftCaseDataService;

    public void setCaseFields(PCSCase pcsCase, long caseReference, State state) {
        if (pcsCase.getEnforcementOrder() == null && State.CASE_ISSUED == state) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
            pcsCase.getEnforcementOrder().setHasUnsubmittedEnforcementData(
                    hasEnforcementDraftData(caseReference) ? YesOrNo.YES : YesOrNo.NO);
        }
    }

    private boolean hasEnforcementDraftData(long caseReference) {
        return draftCaseDataService.hasUnsubmittedCaseData(caseReference, enforceTheOrder);
    }
}
