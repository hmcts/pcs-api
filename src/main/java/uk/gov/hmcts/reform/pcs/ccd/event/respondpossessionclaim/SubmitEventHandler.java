package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private final DraftCaseDataService draftCaseDataService;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        SubmitResponse<State> validationError = validate(caseData, caseReference);
        if (validationError != null) {
            return validationError;
        }

        YesOrNo submitFlag = Optional.ofNullable(caseData.getSubmitDraftAnswers())
            .orElse(YesOrNo.NO);

        if (submitFlag.toBoolean()) {
            return processFinalSubmit(caseReference, caseData);
        } else {
            return processDraftSubmit(caseReference, caseData);
        }
    }

    private SubmitResponse<State> validate(PCSCase caseData, long caseReference) {
        PossessionClaimResponse response = caseData.getPossessionClaimResponse();

        if (response == null) {
            log.error("Submit failed for case {}: possessionClaimResponse is null", caseReference);
            return error("Invalid submission: missing response data");
        }

        return null;
    }

    private SubmitResponse<State> processFinalSubmit(long caseReference, PCSCase caseData) {
        log.info("Processing final submission for case {}", caseReference);

        //TODO: find draft data using idam user and case reference and event

        //TODO: Store defendant response to database
        //This will be implemented in a future ticket.
        //Note that defendants will be stored in a list

        return success();
    }

    private SubmitResponse<State> processDraftSubmit(long caseReference, PCSCase caseData) {
        PossessionClaimResponse response = caseData.getPossessionClaimResponse();

        // Allow partial updates - UI may send only responses OR only contactDetails
        // The merge logic in DraftCaseDataService will preserve existing fields
        if (response.getDefendantProvided() == null) {
            log.error("Draft submit rejected for case {}: defendantProvided is null", caseReference);
            return error("Invalid response structure. Please refresh the page and try again.");
        }

        try {
            saveDraftToDatabase(caseReference, caseData);
            log.debug("Draft saved successfully for case {}", caseReference);
            return success();
        } catch (Exception e) {
            log.error("Failed to save draft for case {}", caseReference, e);
            return error("We couldn't save your response. Please try again or contact support.");
        }
    }

    // Update draft with defendant's latest answers (claimantProvided stays unchanged)
    // Why only defendantProvided? Claimant data is static (came from landlord), defendant data changes
    private void saveDraftToDatabase(long caseReference, PCSCase caseData) {
        log.info("=== Updating draft with latest answers");

        // Extract only defendant's answers (landlord's info doesn't change)
        PossessionClaimResponse defendantAnswersOnly = PossessionClaimResponse.builder()
            .defendantProvided(caseData.getPossessionClaimResponse().getDefendantProvided())
            .build();  // claimantProvided is null - merge logic in DB will preserve existing

        PCSCase draftUpdate = PCSCase.builder()
            .possessionClaimResponse(defendantAnswersOnly)
            .build();  // Only defendant's answers - no case metadata

        draftCaseDataService.patchUnsubmittedEventData(caseReference, draftUpdate, respondPossessionClaim);
        log.info("=== Draft updated for case {}", caseReference);
    }

    private SubmitResponse<State> success() {
        return SubmitResponse.defaultResponse();
    }

    private SubmitResponse<State> error(String errorMessage) {
        return SubmitResponse.<State>builder()
            .errors(List.of(errorMessage))
            .build();
    }
}
