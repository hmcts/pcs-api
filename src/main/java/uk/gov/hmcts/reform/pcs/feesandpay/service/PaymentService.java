package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;
import uk.gov.hmcts.reform.payments.request.CardPaymentServiceRequestDTO;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.CardPaymentServiceRequestResponse;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CardPaymentStatusResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateCardPaymentRequest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateCardPaymentResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateServiceRequestPayload;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateServiceRequestResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.CLAIM;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.GEN_APP_ISSUE;

@Slf4j
@Service
public class PaymentService {

    private final PaymentsClient paymentsClient;
    private final PaymentRequestMapper paymentRequestMapper;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final FeePaymentRepository feePaymentRepository;
    private final CounterClaimRepository counterClaimRepository;
    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final FeeService feeService;
    private final PaymentCallbackStrategyFactory paymentCallbackStrategyFactory;
    private final SecurityContextService securityContextService;
    private final ObjectMapper objectMapper;

    @Value("${payments.api.callback-url}")
    private String callbackUrl;

    @Value("${payments.params.hmctsOrgId}")
    private String hmctsOrgId;

    public PaymentService(PaymentsClient paymentsClient, PaymentRequestMapper paymentRequestMapper,
                          @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider,
                          FeePaymentRepository feePaymentRepository, CounterClaimRepository counterClaimRepository,
                          PcsCaseService pcsCaseService,
                          PartyService partyService, FeeService feeService,
                          PaymentCallbackStrategyFactory paymentCallbackStrategyFactory,
                          SecurityContextService securityContextService, ObjectMapper objectMapper) {
        this.paymentsClient = paymentsClient;
        this.paymentRequestMapper = paymentRequestMapper;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.feePaymentRepository = feePaymentRepository;
        this.counterClaimRepository = counterClaimRepository;
        this.pcsCaseService = pcsCaseService;
        this.partyService = partyService;
        this.feeService = feeService;
        this.paymentCallbackStrategyFactory = paymentCallbackStrategyFactory;
        this.securityContextService = securityContextService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateServiceRequestResponse createServiceRequest(CreateServiceRequestPayload paymentServiceRequest) {
        long caseReference = paymentServiceRequest.getCaseReference();

        FeeType feeType = FeeType.fromCode(paymentServiceRequest.getFeeType());
        FeeDetails feeDetails = feeService.getFee(feeType);
        UUID currentUserId = securityContextService.getCurrentUserId();
        PartyEntity responsibleParty = partyService.getPartyEntityByIdamId(currentUserId, caseReference);
        PaymentCallbackHandlerType callbackHandlerType = getCallbackHandlerType(feeType);
        UUID relatedEntityId = getRelatedEntityId(feeType, caseReference, responsibleParty.getId());

        FeesAndPayTaskData feesAndPayTaskData = FeesAndPayTaskData.builder()
            .caseReference(caseReference)
            .feeDetails(feeDetails)
            .ccdCaseNumber(Long.toString(caseReference))
            .responsiblePartyId(responsibleParty.getId())
            .paymentCallbackHandlerType(callbackHandlerType)
            .relatedEntityId(relatedEntityId)
            .build();

        PaymentServiceResponse paymentServiceResponse = createServiceRequest(feesAndPayTaskData);

        return CreateServiceRequestResponse.builder()
            .serviceRequestReference(paymentServiceResponse.getServiceRequestReference())
            .feeAmount(feeDetails.getFeeAmount())
            .build();
    }

    /**
     * Creates a service request in the Payments API for the given case and fee details.
     * Steps:
     * 1) Maps the provided fee and volume to a Payments {@link FeeDto}.
     * 2) Builds a {@link CasePaymentRequestDto}.
     * 3) Constructs a {@link CreateServiceRequestDTO} including callback URL and HMCTS org ID.
     * 4) Calls {@link PaymentsClient#createServiceRequest(String, CreateServiceRequestDTO)} using the system user
     * token.
     *
     * @param feesAndPayTaskData Holds all the details for the payment to be made and stored
     * @return {@link PaymentServiceResponse} containing the service request reference
     */
    @Transactional
    public PaymentServiceResponse createServiceRequest(FeesAndPayTaskData feesAndPayTaskData) {
        log.info("Building payload for caseReference: {}", feesAndPayTaskData);
        FeeDto feeDto = paymentRequestMapper.toFeeDto(feesAndPayTaskData.getFeeDetails(),
                                                      feesAndPayTaskData.getVolume());
        CasePaymentRequestDto casePaymentRequest = paymentRequestMapper.toCasePaymentRequest(
            getResponsiblePartyName(feesAndPayTaskData));
        log.info("casePaymentRequest: {}", casePaymentRequest);
        long caseReference = feesAndPayTaskData.getCaseReference();
        CreateServiceRequestDTO requestDto = CreateServiceRequestDTO.builder()
            .callBackUrl(callbackUrl)
            .casePaymentRequest(casePaymentRequest)
            .caseReference(String.valueOf(caseReference))
            .ccdCaseNumber(feesAndPayTaskData.getCcdCaseNumber())
            .fees(new FeeDto[]{feeDto})
            .hmctsOrgId(hmctsOrgId)
            .build();
        String feesAndPayTaskDataAsString = writeAsString(feesAndPayTaskData);
        log.info("Calling ServiceCreateRequest with callback url: {} using hmctsOrgId: {} for caseReference: {}",
                 callbackUrl, hmctsOrgId, caseReference);
        PaymentServiceResponse paymentServiceResponse = paymentsClient.createServiceRequest(
            systemUpdateUserTokenProvider.getAuthToken(), requestDto);
        ClaimEntity claimEntity = retrieveClaimEntity(caseReference);
        log.info("Response received for caseReference: {} - Response : {}", caseReference, paymentServiceResponse);
        saveNewFeePayment(feesAndPayTaskDataAsString, feesAndPayTaskData, claimEntity,
                          paymentServiceResponse.getServiceRequestReference());
        return paymentServiceResponse;
    }

    public CreateCardPaymentResponse createPaymentRequest(String serviceRequestReference,
                                                          CreateCardPaymentRequest createCardPaymentRequest) {
        CardPaymentServiceRequestDTO paymentRequest = CardPaymentServiceRequestDTO.builder()
            .amount(createCardPaymentRequest.getAmount())
            .language(createCardPaymentRequest.getLanguage())
            .returnUrl(createCardPaymentRequest.getReturnUrl())
            .build();

        CardPaymentServiceRequestResponse govPayCardPaymentResponse = paymentsClient.createGovPayCardPaymentRequest(
            serviceRequestReference,
            systemUpdateUserTokenProvider.getAuthToken(),
            paymentRequest
        );

        return CreateCardPaymentResponse.builder()
            .paymentReference(govPayCardPaymentResponse.getPaymentReference())
            .status(govPayCardPaymentResponse.getStatus())
            .nextUrl(govPayCardPaymentResponse.getNextUrl())
            .build();
    }

    private PaymentCallbackHandlerType getCallbackHandlerType(FeeType feeType) {
        if (feeType == FeeType.GEN_APP_STANDARD_FEE || feeType == FeeType.GEN_APP_MAX_FEE) {
            return GEN_APP_ISSUE;
        }
        if (isCounterClaimFeeType(feeType)) {
            return COUNTER_CLAIM_ISSUE;
        }
        return CLAIM;
    }

    private UUID getRelatedEntityId(FeeType feeType, long caseReference, UUID responsiblePartyId) {
        if (!isCounterClaimFeeType(feeType)) {
            return null;
        }

        return counterClaimRepository.findFirstByPcsCaseCaseReferenceAndPartyIdAndStatusOrderByClaimSubmittedDateDesc(
                caseReference,
                responsiblePartyId,
                CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED
            )
            .or(() -> counterClaimRepository.findFirstByPcsCaseCaseReferenceAndPartyIdOrderByClaimSubmittedDateDesc(
                caseReference,
                responsiblePartyId
            ))
            .map(CounterClaimEntity::getId)
            .orElseThrow(() -> new IllegalStateException(
                "Counterclaim not found for case %d and party %s".formatted(caseReference, responsiblePartyId)
            ));
    }

    private boolean isCounterClaimFeeType(FeeType feeType) {
        return feeType == FeeType.COUNTER_CLAIM
            || feeType == FeeType.COUNTER_CLAIM_RANGED
            || feeType == FeeType.COUNTER_CLAIM_FLAT_FEE_FEE0450;
    }


    public CardPaymentStatusResponse getPaymentStatus(String paymentReference) {
        PaymentDto govPayCardPaymentStatus = paymentsClient.getGovPayCardPaymentStatus(
            paymentReference,
            systemUpdateUserTokenProvider.getAuthToken()
        );

        return CardPaymentStatusResponse.builder()
            .status(govPayCardPaymentStatus.getStatus())
            .build();
    }

    private String getResponsiblePartyName(FeesAndPayTaskData feesAndPayTaskData) {
        UUID responsiblePartyId = feesAndPayTaskData.getResponsiblePartyId();
        long caseReference = feesAndPayTaskData.getCaseReference();

        PartyEntity responsibleParty = partyService.getPartyEntityByEntityId(responsiblePartyId, caseReference);
        return partyService.getPartyName(responsibleParty);
    }

    private String writeAsString(FeesAndPayTaskData feesAndPayTaskData) {
        try {
            return objectMapper.writeValueAsString(feesAndPayTaskData);
        } catch (IOException e) {
            throw new PaymentException("Unable to write to json the FeesAndPayTaskData", e);
        }
    }

    @Transactional
    public void processPaymentResponse(PaymentStatusCallback paymentStatusCallback) {
        log.info("PaymentStatusCallback status: {}", paymentStatusCallback.getServiceRequestStatus());
        Optional<FeePaymentEntity> byCaseReference = feePaymentRepository
            .findByRequestReference(paymentStatusCallback.getServiceRequestReference());
        if (byCaseReference.isPresent()) {
            FeePaymentEntity feePaymentEntity = byCaseReference.get();
            feePaymentEntity.setExternalReference(paymentStatusCallback.getPaymentReference());
            feePaymentEntity.setPaymentStatus(PaymentStatus.fromValue(paymentStatusCallback.getServiceRequestStatus()));
            PaymentCallbackStrategy paymentCallbackStrategy = paymentCallbackStrategyFactory
                .getStrategy(feePaymentEntity.getPaymentCallbackHandlerType());
            if (paymentCallbackStrategy != null) {
                paymentCallbackStrategy.handle(paymentStatusCallback, feePaymentEntity);
            }
            feePaymentRepository.save(feePaymentEntity);
        } else {
            log.error("Unable to find a payment with the service request reference : {}",
                      paymentStatusCallback.getServiceRequestReference());
        }
    }

    @Transactional
    public void saveNewFeePayment(String feesAndPayTaskDataAsString, FeesAndPayTaskData feesAndPayTaskData,
                                  ClaimEntity claimEntity, String serviceRequestReference) {
        log.info("Saving New Fee Payment for the case: {} with serviceRequestReference: {}",
                 feesAndPayTaskData.getCaseReference(), serviceRequestReference);
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .claim(claimEntity)
            .requestReference(serviceRequestReference)
            .amount(feesAndPayTaskData.getFeeDetails().getFeeAmount())
            .paymentCallbackHandlerType(feesAndPayTaskData.getPaymentCallbackHandlerType())
            .taskData(feesAndPayTaskDataAsString)
            .build();
        feePaymentRepository.save(feePaymentEntity);
    }

    private ClaimEntity retrieveClaimEntity(Long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        // Assuming 1 claim per PcsCase
        return pcsCaseEntity.getClaims().getFirst();
    }

}
