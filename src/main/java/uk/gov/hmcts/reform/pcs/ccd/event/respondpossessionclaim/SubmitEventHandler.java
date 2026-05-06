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
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private final DraftCaseDataService draftCaseDataService;
    private final ClaimResponseService claimResponseService;
    private final DefendantResponseService defendantResponseService;
    private final NotificationService notificationService;

    private static final String PENDING_CASE_ISSUED = "PENDING_CASE_ISSUED";

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        log.info("RespondPossessionClaim submit callback invoked for Case Reference: {}", caseReference);
        return processFinalSubmit(caseReference);
    }

    private SubmitResponse<State> processFinalSubmit(long caseReference) {
        log.info("Processing final submission for case {}", caseReference);

        //load draft data
        PCSCase draftData = draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));

        //get only possession response from draft
        PossessionClaimResponse responseDraftData = draftData.getPossessionClaimResponse();

        //validate draft data
        SubmitResponse<State> validationError = validate(responseDraftData, caseReference);
        if (validationError != null) {
            return validationError;
        }

        //call services to save to relevant tables
        claimResponseService.saveDraftData(responseDraftData, caseReference);

        DefendantResponseEntity defendantResponse =
            defendantResponseService.saveDefendantResponse(caseReference, responseDraftData);

        //delete draft as it's no longer needed
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim);

        // sends email notification to defendant
        sendEmailNotification(defendantResponse);

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

    protected void sendEmailNotification(DefendantResponseEntity defendantResponse) {
        CounterClaimEntity counterClaim = getAssociatedCounterClaim(defendantResponse);
        if (counterClaim == null) {
            log.info("Sending no counter claim email for defendant response {}",
                     defendantResponse.getId());
            notificationService.sendDefendantResponseNoCounterclaimEmailNotification(defendantResponse);
            return;
        }

        if (!PENDING_CASE_ISSUED.equals(counterClaim.getStatus())) {
            log.info("Counterclaim status not eligible for email. status={}, defendantResponseId={}",
                     counterClaim.getStatus(), defendantResponse.getId());
            return;
        }

        boolean hasHwfReference = counterClaim.getHwfReferenceNumber() != null
            && !counterClaim.getHwfReferenceNumber().isBlank();

        if (!hasHwfReference) {
            log.info("Sending counterclaim payment required email for defendant response {}",
                     defendantResponse.getId());
            notificationService.sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(defendantResponse);
            return;
        }

        log.info("Sending counterclaim no payment required email for defendant response {}",
                 defendantResponse.getId());
        notificationService.sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(defendantResponse);
    }

    private CounterClaimEntity getAssociatedCounterClaim(DefendantResponseEntity defendantResponse) {
        UUID partyId = defendantResponse.getParty().getId();
        PcsCaseEntity pcsCase = defendantResponse.getPcsCase();

        return pcsCase.getCounterClaims().stream()
            .filter(counterClaim -> counterClaim.getParty().getId().equals(partyId))
            .findFirst()
            .orElse(null);
    }
}
