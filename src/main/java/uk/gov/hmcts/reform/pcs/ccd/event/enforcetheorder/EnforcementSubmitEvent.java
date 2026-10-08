package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;

@Component("enforcementSubmitEventHandler")
@Slf4j
@RequiredArgsConstructor
public class EnforcementSubmitEvent implements Submit<PCSCase, State> {

    private final EnforcementOrderService enforcementOrderService;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> payload) {
        long caseReference = payload.caseReference();
        enforcementOrderService.saveAndClearDraftData(caseReference, payload.caseData().getEnforcementOrder());
        log.debug("Saved submitted enforcement order data and deleted draft data for case reference {}", caseReference);
        return SubmitResponse.defaultResponse();
    }

}
