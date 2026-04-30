package uk.gov.hmcts.reform.pcs.ccd.event.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmitDashboardViewHandler implements Submit<PCSCase, State> {

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        log.info("DashboardView SUBMIT invoked for caseReference={} (no-op submit, read-only event)", caseReference);
        // READ-ONLY: the dashboard view event should not persist changes.
        return SubmitResponse.defaultResponse();
    }
}

