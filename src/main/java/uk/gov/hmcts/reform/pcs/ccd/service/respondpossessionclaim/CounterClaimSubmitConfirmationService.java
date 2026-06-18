package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RespondPossessionClaimSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;

import java.math.BigDecimal;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE;

@Service
@RequiredArgsConstructor
public class CounterClaimSubmitConfirmationService {

    private final PartyService partyService;
    private final FeeService feeService;
    private final PaymentService paymentService;
    private final CounterClaimFeeCalculator counterClaimFeeCalculator;
    private final SecurityContextService securityContextService;
    private final ObjectMapper objectMapper;

    public SubmitResponse<State> buildSubmitResponse(
        long caseReference,
        RespondPossessionClaimSubmitPersistenceResult persistenceResult
    ) {
        CounterClaimEntity counterClaimEntity = persistenceResult.counterClaimEntity();
        if (counterClaimEntity == null) {
            return SubmitResponse.defaultResponse();
        }

        if (persistenceResult.issuedWithoutPayment()) {
            return buildCounterClaimConfirmationResponse(
                persistenceResult.counterClaimEntity().getStatus(),
                null,
                null,
                null
            );
        }

        CounterClaim counterClaim = persistenceResult.possessionClaimResponse()
            .getDefendantResponses()
            .getCounterClaim();
        FeeType feeType = counterClaimFeeCalculator.resolveFeeType(counterClaim);
        FeeDetails feeDetails = feeService.getFee(
            feeType,
            counterClaimFeeCalculator.resolveFeeLookupAmountInPounds(counterClaim)
        );
        String serviceRequestReference = createPaymentServiceRequest(
            counterClaimEntity,
            feeDetails,
            caseReference
        );

        return buildCounterClaimConfirmationResponse(
            CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED,
            serviceRequestReference,
            feeDetails.getFeeAmount(),
            counterClaim.getClaimType()
        );
    }

    private String createPaymentServiceRequest(CounterClaimEntity counterClaimEntity,
                                               FeeDetails feeDetails,
                                               long caseReference) {
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
        return paymentServiceResponse.getServiceRequestReference();
    }

    private SubmitResponse<State> buildCounterClaimConfirmationResponse(
        CounterClaimState status,
        String serviceRequestReference,
        BigDecimal feeAmount,
        CounterClaimType claimType
    ) {
        CounterClaimSubmitResponse.CounterClaimSubmitResponseBuilder counterClaimResponseBuilder =
            CounterClaimSubmitResponse.builder().status(status);
        if (serviceRequestReference != null) {
            counterClaimResponseBuilder.serviceRequestReference(serviceRequestReference);
        }
        if (feeAmount != null) {
            counterClaimResponseBuilder.feeAmount(feeAmount);
        }
        if (claimType != null) {
            counterClaimResponseBuilder.claimType(claimType);
        }

        RespondPossessionClaimSubmitResponse response = RespondPossessionClaimSubmitResponse.builder()
            .counterClaim(counterClaimResponseBuilder.build())
            .build();

        return SubmitResponse.<State>builder()
            .confirmationBody(writeAsString(response))
            .build();
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
}
