package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RespondPossessionClaimSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimFeeCalculator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE;

@Component("respondToClaimSubmitEventHandler")
@Slf4j
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private final DraftCaseDataService draftCaseDataService;
    private final ClaimResponseService claimResponseService;
    private final DefendantResponseService defendantResponseService;
    private final CounterClaimService counterClaimService;
    private final SecurityContextService securityContextService;
    private final PartyService partyService;
    private final FeeService feeService;
    private final PaymentService paymentService;
    private final CounterClaimFeeCalculator counterClaimFeeCalculator;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        log.info("RespondPossessionClaim submit callback invoked for Case Reference: {}", caseReference);
        return processFinalSubmit(caseReference);
    }

    private SubmitResponse<State> processFinalSubmit(long caseReference) {
        log.info("Processing final submission for case {}", caseReference);

        PCSCase draftData = draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));

        PossessionClaimResponse responseDraftData = draftData.getPossessionClaimResponse();

        SubmitResponse<State> validationError = validate(responseDraftData, caseReference);
        if (validationError != null) {
            return validationError;
        }

        claimResponseService.saveDraftData(responseDraftData, caseReference);
        defendantResponseService.saveDefendantResponse(caseReference, responseDraftData);

        CounterClaim counterClaim = responseDraftData.getDefendantResponses().getCounterClaim();
        Optional<CounterClaimEntity> savedCounterClaim = counterClaimService.saveCounterClaim(caseReference, counterClaim);

        SubmitResponse<State> submitResponse = buildSubmitResponse(
            caseReference,
            responseDraftData,
            savedCounterClaim.orElse(null)
        );

        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim);
        log.info("Successfully saved defendant response for case: {}", caseReference);
        return submitResponse;
    }

    private SubmitResponse<State> validate(PossessionClaimResponse possessionClaimResponse, long caseReference) {
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

    private SubmitResponse<State> buildSubmitResponse(
        long caseReference,
        PossessionClaimResponse possessionClaimResponse,
        CounterClaimEntity counterClaimEntity
    ) {
        if (!isCounterClaimPaymentRequired(possessionClaimResponse)) {
            return SubmitResponse.defaultResponse();
        }

        if (counterClaimEntity == null) {
            throw new IllegalStateException("Counterclaim entity required for payment flow");
        }

        CounterClaim counterClaim = possessionClaimResponse.getDefendantResponses().getCounterClaim();
        FeeType feeType = counterClaimFeeCalculator.resolveFeeType(counterClaim);
        FeeDetails feeDetails = feeService.getFee(feeType);
        PartyEntity responsibleParty = getCurrentUserParty(caseReference);

        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeDetails(feeDetails)
            .ccdCaseNumber(String.valueOf(caseReference))
            .caseReference(caseReference)
            .responsiblePartyId(responsibleParty.getId())
            .paymentCallbackHandlerType(COUNTER_CLAIM_ISSUE)
            .relatedEntityId(counterClaimEntity.getId())
            .build();

        PaymentServiceResponse paymentServiceResponse = paymentService.createServiceRequest(taskData);

        CounterClaimSubmitResponse counterClaimSubmitResponse = CounterClaimSubmitResponse.builder()
            .status(CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED)
            .serviceRequestReference(paymentServiceResponse.getServiceRequestReference())
            .feeAmount(feeDetails.getFeeAmount())
            .build();

        RespondPossessionClaimSubmitResponse response = RespondPossessionClaimSubmitResponse.builder()
            .counterClaim(counterClaimSubmitResponse)
            .build();

        return SubmitResponse.<State>builder()
            .confirmationBody(writeAsString(response))
            .build();
    }

    private boolean isCounterClaimPaymentRequired(PossessionClaimResponse possessionClaimResponse) {
        if (possessionClaimResponse == null || possessionClaimResponse.getDefendantResponses() == null) {
            return false;
        }
        if (possessionClaimResponse.getDefendantResponses().getMakeCounterClaim() != VerticalYesNo.YES) {
            return false;
        }
        CounterClaim counterClaim = possessionClaimResponse.getDefendantResponses().getCounterClaim();
        if (counterClaim == null) {
            return false;
        }
        String hwfReference = counterClaim.getHwfReferenceNumber();
        return hwfReference == null || hwfReference.trim().isEmpty();
    }

    private PartyEntity getCurrentUserParty(long caseReference) {
        UUID currentUserId = securityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("Current user IDAM ID is null");
        }
        return partyService.getPartyEntityByIdamId(currentUserId, caseReference);
    }

    private String writeAsString(RespondPossessionClaimSubmitResponse submitResponse) {
        try {
            return objectMapper.writeValueAsString(submitResponse);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise respond possession claim submit response", e);
        }
    }

    private SubmitResponse<State> error(String errorMessage) {
        return SubmitResponse.<State>builder()
            .errors(List.of(errorMessage))
            .build();
    }
}
