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

    /**
     * The final submit must carry the draft version the citizen reviewed (posted from the review page); if the
     * stored draft has moved on since, nothing is persisted and the citizen is asked to review again.
     * A missing posted version is tolerated (pre-migration drafts, review pages rendered before deploy) and
     * only logged. (HDPI-8866 W05)
     */
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
