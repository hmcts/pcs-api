package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

public interface RespondPossessionClaimSubmissionEventStrategy {

    boolean supports(List<String> roles);

    SubmitResponse<State> process(EventPayload<PCSCase, State> eventPayload);
}
