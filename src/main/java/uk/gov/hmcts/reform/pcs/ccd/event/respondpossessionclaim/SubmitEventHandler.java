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

        log.info("RespondPossessionClaim submit callback invoked for Case Reference: {}", caseReference);

        SubmitResponse<State> validationError = validate(caseData, caseReference);
        if (validationError != null) {
            return validationError;
        }

        // Check if defendant clicked "Submit" (final) or "Save and come back later" (draft)
        YesOrNo submitFlag = Optional.ofNullable(caseData.getSubmitDraftAnswers())
            .orElse(YesOrNo.NO);

        if (submitFlag.toBoolean()) {
            return processFinalSubmit(caseReference, caseData);
        }

        return processDraftSubmit(caseReference, caseData);
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

        // Validate defendantData exists (UI should always send this structure)
        if (response.getDefendantData() == null) {
            log.error("Draft submit rejected for case {}: defendantData is null", caseReference);
            return error("Invalid response structure. Please refresh the page and try again.");
        }

        try {
            saveDraftToDatabase(caseReference, caseData);
            return success();
        } catch (Exception e) {
            log.error("Failed to save draft for case {}", caseReference, e);
            return error("We couldn't save your response. Please try again or contact support.");
        }
    }

    private void saveDraftToDatabase(long caseReference, PCSCase caseData) {
        PCSCase partialUpdate = buildDefendantOnlyUpdate(caseData);
        draftCaseDataService.patchUnsubmittedEventData(caseReference, partialUpdate, respondPossessionClaim);
    }

    /**
     * Builds partial update containing ONLY defendant's answers.
     *
     * <p>
     * Why partial? UI may send only defendant responses OR only contact details.
     * Partial update preserves existing fields via deep merge:
     * - Sends: defendantData only
     * - patchUnsubmittedEventData merges: preserves existing defendantData fields
     * - Result: defendant's new answers merged with existing defendant data
     */
    private PCSCase buildDefendantOnlyUpdate(PCSCase caseData) {
        PossessionClaimResponse defendantAnswersOnly = PossessionClaimResponse.builder()
            .defendantData(caseData.getPossessionClaimResponse().getDefendantData())
            .build();

        return PCSCase.builder()
            .possessionClaimResponse(defendantAnswersOnly)
            .build();  // Sparse object - other fields preserved by patchUnsubmittedEventData
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
