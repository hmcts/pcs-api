package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public interface RespondPossessionClaimSubmissionEventStrategy {

    boolean supports(boolean citizenUser);

    SubmitResponse<State> process(long caseReference);
}
