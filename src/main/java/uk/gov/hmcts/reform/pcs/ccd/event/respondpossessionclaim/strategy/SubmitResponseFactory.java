package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;

import java.util.List;

@Component
@Slf4j
public class SubmitResponseFactory {

    public SubmitResponse<State> validate(PossessionClaimResponse possessionClaimResponse, long caseReference) {

        if (possessionClaimResponse == null) {
            log.error("Submit failed for case {}: possession claim response is null", caseReference);
            return error("Invalid submission: missing response data");
        }

        if (possessionClaimResponse.getDefendantResponses() == null) {
            log.error("Submit failed for case {}: defendant responses is null", caseReference);
            return error("Invalid submission: missing defendant response data");
        }

        return null;
    }

    public SubmitResponse<State> success() {
        return SubmitResponse.defaultResponse();
    }

    public SubmitResponse<State> error(String message) {
        return SubmitResponse.<State>builder()
            .errors(List.of(message))
            .build();
    }
}
