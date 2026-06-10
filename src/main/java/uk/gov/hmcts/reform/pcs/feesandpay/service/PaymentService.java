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
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CardPaymentStatusResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateCardPaymentRequest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateCardPaymentResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
public class PaymentService {

    private final PaymentsClient paymentsClient;
    private final PaymentRequestMapper paymentRequestMapper;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final FeePaymentRepository feePaymentRepository;
    private final PcsCaseService pcsCaseService;
    private final PaymentCallbackStrategyFactory paymentCallbackStrategyFactory;
    private final ObjectMapper objectMapper;

    @Value("${payments.api.callback-url}")
    private String callbackUrl;

    @Value("${payments.params.hmctsOrgId}")
    private String hmctsOrgId;

    public PaymentService(PaymentsClient paymentsClient, PaymentRequestMapper paymentRequestMapper,
        @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider,
        FeePaymentRepository feePaymentRepository, PcsCaseService pcsCaseService,
        PaymentCallbackStrategyFactory paymentCallbackStrategyFactory, ObjectMapper objectMapper) {
        this.paymentsClient = paymentsClient;
        this.paymentRequestMapper = paymentRequestMapper;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.feePaymentRepository = feePaymentRepository;
        this.pcsCaseService = pcsCaseService;
        this.paymentCallbackStrategyFactory = paymentCallbackStrategyFactory;
        this.objectMapper = objectMapper;
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
            feesAndPayTaskData.getResponsiblePartyName());

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

        FeePaymentEntity feePaymentEntity = feePaymentRepository.findByServiceRequestReference(serviceRequestReference)
            .orElseThrow(
                () -> new FeePaymentNotFoundException("No fee payment entity found for " + serviceRequestReference)
            );

        CardPaymentServiceRequestDTO paymentRequest = CardPaymentServiceRequestDTO.builder()
            .amount(createCardPaymentRequest.getAmount())
            .language(createCardPaymentRequest.getLanguage())
            .returnUrl(createCardPaymentRequest.getReturnUrl())
            .build();

        if (feePaymentEntity.getPaymentStatus() != null) {
            throw new IllegalStateException("Service request " + serviceRequestReference
                                                + " already has a completed status");
        }

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


    public CardPaymentStatusResponse getPaymentStatus(String internalReference) {
        PaymentDto govPayCardPaymentStatus = paymentsClient.getGovPayCardPaymentStatusWithCallback(
            internalReference,
            systemUpdateUserTokenProvider.getAuthToken()
        );

        return CardPaymentStatusResponse.builder()
            .status(govPayCardPaymentStatus.getStatus())
            .build();
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

        getFeePaymentEntity(paymentStatusCallback.getServiceRequestReference())
            .ifPresent(
                feePaymentEntity -> {
                    feePaymentEntity.setExternalReference(paymentStatusCallback.getPaymentReference());
                    feePaymentEntity
                        .setPaymentStatus(PaymentStatus.fromValue(paymentStatusCallback.getServiceRequestStatus()));

                    callPaymentCallbackHandler(paymentStatusCallback, feePaymentEntity);

                    feePaymentRepository.save(feePaymentEntity);
                });
    }

    private void callPaymentCallbackHandler(PaymentStatusCallback paymentStatusCallback,
                                            FeePaymentEntity feePaymentEntity) {

        PaymentCallbackStrategy paymentCallbackStrategy = paymentCallbackStrategyFactory
            .getStrategy(feePaymentEntity.getPaymentCallbackHandlerType());

        if (paymentCallbackStrategy != null) {
            paymentCallbackStrategy.handle(paymentStatusCallback, feePaymentEntity);
        } else {
            log.warn("No handler found for type {}", feePaymentEntity.getPaymentCallbackHandlerType());
        }
    }

    @Transactional
    public void saveNewFeePayment(String feesAndPayTaskDataAsString, FeesAndPayTaskData feesAndPayTaskData,
                                  ClaimEntity claimEntity, String serviceRequestReference) {
        log.info("Saving New Fee Payment for the case: {} with serviceRequestReference: {}",
                 feesAndPayTaskData.getCaseReference(), serviceRequestReference);
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .claim(claimEntity)
            .serviceRequestReference(serviceRequestReference)
            .amount(feesAndPayTaskData.getFeeDetails().getFeeAmount())
            .paymentCallbackHandlerType(feesAndPayTaskData.getPaymentCallbackHandlerType())
            .taskData(feesAndPayTaskDataAsString)
            .relatedEntityId(feesAndPayTaskData.getRelatedEntityId())
            .build();
        feePaymentRepository.save(feePaymentEntity);
    }

    private ClaimEntity retrieveClaimEntity(Long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        // Assuming 1 claim per PcsCase
        return pcsCaseEntity.getClaims().getFirst();
    }

    private Optional<FeePaymentEntity> getFeePaymentEntity(String serviceRequestReference) {
        Optional<FeePaymentEntity> optionalFeePaymentEntity = feePaymentRepository
            .findByServiceRequestReference(serviceRequestReference);

        if (optionalFeePaymentEntity.isEmpty()) {
            log.error("Unable to find a payment with the service request reference : {}", serviceRequestReference);
        }

        return optionalFeePaymentEntity;
    }

}
