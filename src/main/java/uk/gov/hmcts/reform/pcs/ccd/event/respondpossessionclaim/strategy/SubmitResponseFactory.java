package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.exception.DraftVersionConflictException;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class SubmitResponseFactory {

    public Optional<SubmitResponse<State>> validate(PossessionClaimResponse possessionClaimResponse,
                                                    long caseReference) {

        if (possessionClaimResponse == null) {
            log.error("Submit failed for case {}: possession claim response is null", caseReference);
            return Optional.of(error("Invalid submission: missing response data"));
        }

        if (possessionClaimResponse.getDefendantResponses() == null) {
            log.error("Submit failed for case {}: defendant responses is null", caseReference);
            return Optional.of(error("Invalid submission: missing defendant response data"));
        }

        return Optional.empty();
    }

    // A missing reviewed version is tolerated so review pages rendered before this shipped can still submit.
    public Optional<SubmitResponse<State>> validateReviewedDraftVersion(Long reviewedVersion,
                                                                       Long currentVersion,
                                                                       long caseReference) {
        if (reviewedVersion == null) {
            log.warn("Submit for case {} carried no reviewed draft version; skipping the binding check",
                     caseReference);
            return Optional.empty();
        }
        if (!reviewedVersion.equals(currentVersion)) {
            log.warn("Submit rejected for case {}: reviewed draft version {} but stored draft is at {}",
                     caseReference, reviewedVersion, currentVersion);
            return Optional.of(error(DraftVersionConflictException.ERROR_MESSAGE));
        }
        return Optional.empty();
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
