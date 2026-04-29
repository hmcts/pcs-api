package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private final DraftCaseDataService draftCaseDataService;
    private final ClaimResponseService claimResponseService;
    private final DefendantResponseService defendantResponseService;
    private final SecurityContextService securityContextService;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        log.info("RespondPossessionClaim submit callback invoked for Case Reference: {}", caseReference);
        return processFinalSubmit(caseReference, eventPayload.caseData());
    }

    private SubmitResponse<State> processFinalSubmit(long caseReference, PCSCase caseData) {
        log.info("Processing final submission for case {}", caseReference);
        boolean citizenUser = securityContextService.getCurrentUserDetails().getRoles()
            .contains(UserRole.CITIZEN.getRole());
        UUID representedPartyId = citizenUser ? null : getRequiredPartyId(caseData);

        //load draft data
        PCSCase draftData = (representedPartyId == null
            ? draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            : draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim, representedPartyId))
            .orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));

        //get only possession response from draft
        PossessionClaimResponse responseDraftData = draftData.getPossessionClaimResponse();

        //validate draft data
        SubmitResponse<State> validationError = validate(responseDraftData, caseReference);
        if (validationError != null) {
            return validationError;
        }

        //call services to save to relevant tables
        if (representedPartyId == null) {
            claimResponseService.saveDraftData(responseDraftData, caseReference);
            defendantResponseService.saveDefendantResponse(caseReference, responseDraftData);
            draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim);
        } else {
            claimResponseService.saveDraftDataForParty(responseDraftData, caseReference, representedPartyId);
            defendantResponseService.saveDefendantResponse(caseReference, responseDraftData, representedPartyId);
            draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim, representedPartyId);
        }

        log.info("Successfully saved defendant response for case: {}", caseReference);
        return success();
    }

    private SubmitResponse<State> validate(PossessionClaimResponse possessionClaimResponse, long caseReference) {
        if (possessionClaimResponse == null) {
            log.error("Submit failed for case {}: possession claim response is null", caseReference);
            return error("Invalid submission: missing response data");
        }

        // Only persist the defendant response and its related entities if there is actual defendant response draft data
        if (possessionClaimResponse.getDefendantResponses() == null) {
            log.error("Submit failed for case {}: defendant responses is null", caseReference);
            return error("Invalid submission: missing defendant response data");
        }
        return null;
    }

    private SubmitResponse<State> success() {
        return SubmitResponse.defaultResponse();
    }

    private SubmitResponse<State> error(String errorMessage) {
        return SubmitResponse.<State>builder()
            .errors(List.of(errorMessage))
            .build();
    }

    private UUID getRequiredPartyId(PCSCase caseData) {
        String selectedPartyId = caseData.getSelectedRespondingPartyId();
        if (isBlank(selectedPartyId)) {
            throw new IllegalStateException("Missing required represented party context for respond to claim");
        }

        try {
            return UUID.fromString(selectedPartyId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid selected responding party id for respond to claim", ex);
        }
    }
}
