package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private final DraftCaseDataService draftCaseDataService;
    private final ClaimResponseService claimResponseService;
    private final DefendantResponseService defendantResponseService;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        //extract data from event
        PossessionClaimResponse defendantResponse = eventPayload.caseData().getPossessionClaimResponse();

        log.info("RespondPossessionClaim submit callback invoked for Case Reference: {}", caseReference);

        SubmitResponse<State> validationError = validate(defendantResponse, caseReference);
        if (validationError != null) {
            return validationError;
        }
        return processFinalSubmit(caseReference);
    }

    private SubmitResponse<State> validate(PossessionClaimResponse possessionClaimResponse, long caseReference) {
        if (possessionClaimResponse == null) {
            log.error("Submit failed for case {}: possessionClaimResponse is null", caseReference);
            return error("Invalid submission: missing response data");
        }

        return null;
    }

    private SubmitResponse<State> processFinalSubmit(long caseReference) {
        log.info("Processing final submission for case {}", caseReference);

        //load draft data
        PCSCase draftData = draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));

        //get only possession response from draft
        PossessionClaimResponse responseDraftData = draftData.getPossessionClaimResponse();

        //call services to save to relevant tables
        claimResponseService
            .saveDraftData(responseDraftData);

        //save defendant responses to defendant_response table
        defendantResponseService.saveDefendantResponse(caseReference,
            responseDraftData.getDefendantResponses());

        //delete draft as it's no longer needed
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim);

        log.info("Successfully saved defendant response for case: {}", caseReference);
        return success();
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
