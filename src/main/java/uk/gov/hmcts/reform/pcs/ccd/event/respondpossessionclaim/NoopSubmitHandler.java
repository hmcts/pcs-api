package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public class NoopSubmitHandler implements Submit<PCSCase, State> {

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> payload) {
        return SubmitResponse.defaultResponse();
    }
}
