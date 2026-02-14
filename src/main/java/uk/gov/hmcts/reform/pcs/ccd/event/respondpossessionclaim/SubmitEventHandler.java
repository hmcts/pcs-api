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
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ImmutablePartyFieldValidator;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private final DraftCaseDataService draftCaseDataService;
    private final ImmutablePartyFieldValidator immutableFieldValidator;

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
            log.error("Sub mit failed for case {}: possessionClaimResponse is null", caseReference);
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

        // Validate at least one of contact details or responses is provided
        // Frontend can send: only contact, only responses, or both
        if (response.getDefendantContactDetails() == null
            && response.getDefendantResponses() == null) {
            log.error("Draft submit rejected for case {}: both defendantContactDetails and defendantResponses are null",
                caseReference);
            return error("Invalid submission: no data to save");
        }

        // Validate immutable fields are not sent when contact details provided
        if (response.getDefendantContactDetails() != null
            && response.getDefendantContactDetails().getParty() != null) {

            List<String> violations = immutableFieldValidator.findImmutableFieldViolations(
                response.getDefendantContactDetails().getParty(),
                caseReference
            );

            if (!violations.isEmpty()) {
                log.error("Draft submit rejected for case {}: immutable field violations: {}",
                    caseReference, violations);

                List<String> errors = violations.stream()
                    .map(field -> "Invalid submission: immutable field must not be sent: " + field)
                    .toList();

                return SubmitResponse.<State>builder()
                    .errors(errors)
                    .build();
            }
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
     * Builds partial update containing ONLY defendant's contact details and responses.
     *
     * <p>
     * Why partial? UI may send only defendant responses OR only contact details.
     * Partial update preserves existing fields via deep merge:
     * - Sends: defendant contact details and responses only
     * - patchUnsubmittedEventData merges: preserves existing fields
     * - Result: defendant's new answers merged with existing defendant data
     */
    private PCSCase buildDefendantOnlyUpdate(PCSCase caseData) {
        PossessionClaimResponse response = caseData.getPossessionClaimResponse();

        PossessionClaimResponse defendantAnswersOnly = PossessionClaimResponse.builder()
            .defendantContactDetails(response.getDefendantContactDetails())
            .defendantResponses(response.getDefendantResponses())
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
