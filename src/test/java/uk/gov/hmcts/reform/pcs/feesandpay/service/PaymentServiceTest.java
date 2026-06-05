package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CardPaymentStatusResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateCardPaymentRequest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateCardPaymentResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.CLAIM;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final long CASE_REFERENCE = 123L;
    private static final String CCD_CASE_NUMBER = "1111-2222-3333-4444";
    private static final int VOLUME = 2;
    private static final String RESPONSIBLE_PARTY = "Applicant";
    private static final UUID RESPONSIBLE_PARTY_ID = UUID.randomUUID();
    private static final String SYSTEM_TOKEN = "Bearer sys-token";
    private static final BigDecimal CALCULATED_AMOUNT = new BigDecimal("808.00");
    private static final String SERVICE_REQUEST_REFERENCE = "SR-123";
    private static final String CALLBACK_URL = "https://etc:123/service-request-update";
    private static final String HMCTS_ORG_ID = "TEST_ORG";

    @Mock
    private PaymentsClient paymentsClient;
    @Mock
    private PaymentRequestMapper paymentRequestMapper;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private FeePaymentRepository feePaymentRepository;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private PaymentCallbackStrategyFactory paymentCallbackStrategyFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Captor
    private ArgumentCaptor<CreateServiceRequestDTO> createServiceRequestCaptor;
    @Captor
    private ArgumentCaptor<CardPaymentServiceRequestDTO> cardPaymentRequestCaptor;

    @InjectMocks
    private PaymentService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(SYSTEM_TOKEN);

        setPrivateField(underTest, "callbackUrl", CALLBACK_URL);
        setPrivateField(underTest, "hmctsOrgId", HMCTS_ORG_ID);
        setPrivateField(underTest, "objectMapper", objectMapper);
    }

    @Nested
    @DisplayName("Create service request from Task Data")
    class CreateServiceRequestFromTaskDataTests {

        @Test
        void shouldCreateServiceRequestSuccessfully() {
            // Given
            FeeDetails feeDetails = Instancio.create(FeeDetails.class);
            paymentsClientDependencies(feeDetails);
            stubPcsCaseEntity();
            PaymentServiceResponse expectedResponse = createPaymentServiceResponse();
            when(paymentsClient.createServiceRequest(any(), any(CreateServiceRequestDTO.class)))
                .thenReturn(expectedResponse);
            FeesAndPayTaskData feesAndPayTaskData = createFeesAndPayTaskData(feeDetails);

            stubResponsibleParty();

            // When
            PaymentServiceResponse result = underTest.createServiceRequest(feesAndPayTaskData);

            // Then
            assertServiceRequestCreation(feesAndPayTaskData, result);
        }

        @Test
        void shouldCreateServiceRequest_NoPCSCase() {
            // Given
            FeeDetails feeDetails = Instancio.create(FeeDetails.class);
            when(pcsCaseService.loadCase(anyLong())).thenThrow(new CaseNotFoundException(222L));

            // When
            assertThatThrownBy(() -> underTest.createServiceRequest(createFeesAndPayTaskData(feeDetails)))
                .isInstanceOf(CaseNotFoundException.class);
        }

        @Test
        void shouldPersistFeePaymentWhenCreatingServiceRequest() {
            // Given
            FeeDetails feeDetails = createFeeDetails();
            paymentsClientDependencies(feeDetails);
            PcsCaseEntity pcsCaseEntity = setupPcsCase(claimPartyEntity());
            when(pcsCaseService.loadCase(anyLong())).thenReturn(pcsCaseEntity);
            when(paymentsClient.createServiceRequest(any(), any(CreateServiceRequestDTO.class)))
                .thenReturn(createPaymentServiceResponse());
            FeesAndPayTaskData feesAndPayTaskData = createFeesAndPayTaskData(feeDetails);

            stubResponsibleParty();

            // When
            underTest.createServiceRequest(feesAndPayTaskData);

            // Then
            ArgumentCaptor<FeePaymentEntity> captor = ArgumentCaptor.forClass(FeePaymentEntity.class);
            verify(feePaymentRepository).save(captor.capture());
            FeePaymentEntity saved = captor.getValue();

            assertThat(saved.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST_REFERENCE);
            assertThat(saved.getAmount()).isEqualByComparingTo(CALCULATED_AMOUNT);
            assertThat(saved.getClaim()).isSameAs(pcsCaseEntity.getClaims().getFirst());
        }

        @Test
        void shouldThrowPaymentExceptionWhenFeesAndPayTaskDataCannotBeSerialisedToJson() throws Exception {
            // Given
            FeeDetails feeDetails = createFeeDetails();
            ObjectMapper mapper = mock(ObjectMapper.class);
            when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Unable to write to json") {});
            setPrivateField(underTest, "objectMapper", mapper);
            FeesAndPayTaskData feesAndPayTaskData = createFeesAndPayTaskData(feeDetails);
            paymentsClientDependencies(feeDetails);
            stubResponsibleParty();

            // When / Then
            assertThatExceptionOfType(PaymentException.class)
                .isThrownBy(() -> underTest.createServiceRequest(feesAndPayTaskData))
                .withMessageContaining("Unable to write to json");
        }
    }

    @Nested
    @DisplayName("Process payment request tests")
    class ProcessPaymentRequestTests {

        @Test
        void shouldProcessPaymentResponse() {
            // Given
            String requestReference = UUID.randomUUID().toString();
            String paymentReference = UUID.randomUUID().toString();
            Payment payment = Payment.builder().paymentReference(paymentReference).build();
            PaymentStatusCallback paymentStatusCallback = PaymentStatusCallback.builder()
                .serviceRequestReference(requestReference).serviceRequestStatus(PaymentStatus.PAID.getValue())
                .payment(payment).build();

            FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.PAID)
                .serviceRequestReference(requestReference)
                .externalReference(paymentReference)
                .paymentCallbackHandlerType(CLAIM)
                .build();

            when(feePaymentRepository.findByServiceRequestReference(requestReference))
                .thenReturn(Optional.of(feePaymentEntity));
            when(paymentCallbackStrategyFactory.getStrategy(any(PaymentCallbackHandlerType.class)))
                .thenReturn(mock(MakeAClaimPaymentCallbackHandler.class));

            // When
            underTest.processPaymentResponse(paymentStatusCallback);

            // Then
            verify(feePaymentRepository).findByServiceRequestReference(requestReference);
            ArgumentCaptor<FeePaymentEntity> feePaymentCaptor = ArgumentCaptor.forClass(FeePaymentEntity.class);
            verify(feePaymentRepository).save(feePaymentCaptor.capture());
            FeePaymentEntity paymentEntity = feePaymentCaptor.getValue();

            assertThat(paymentEntity.getServiceRequestReference()).isEqualTo(requestReference);
            assertThat(paymentEntity.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(paymentEntity.getExternalReference()).isEqualTo(paymentReference);
        }

        @Test
        void shouldNotUpdateFeePaymentWhenRequestReferenceNotFound() {
            // Given
            String requestReference = UUID.randomUUID().toString();
            PaymentStatusCallback paymentStatusCallback = PaymentStatusCallback.builder()
                .serviceRequestReference(requestReference)
                .serviceRequestStatus(PaymentStatus.PAID.getValue())
                .payment(Payment.builder().paymentReference(UUID.randomUUID().toString()).build())
                .build();
            when(feePaymentRepository.findByServiceRequestReference(requestReference)).thenReturn(Optional.empty());

            // When
            underTest.processPaymentResponse(paymentStatusCallback);

            // Then
            verify(feePaymentRepository).findByServiceRequestReference(requestReference);
            verify(feePaymentRepository, never()).save(any(FeePaymentEntity.class));
        }

        @Test
        void shouldCallPaymentCallbackStrategyWhenStrategyExistsForJourneyId() {
            // Given
            String requestReference = UUID.randomUUID().toString();
            String paymentReference = UUID.randomUUID().toString();
            Payment payment = Payment.builder().paymentReference(paymentReference).build();
            PaymentStatusCallback paymentStatusCallback = PaymentStatusCallback.builder()
                .serviceRequestReference(requestReference).serviceRequestStatus(PaymentStatus.PAID.getValue())
                .payment(payment).build();
            FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().paymentStatus(PaymentStatus.PAID)
                .serviceRequestReference(requestReference).paymentCallbackHandlerType(CLAIM).build();
            when(feePaymentRepository.findByServiceRequestReference(requestReference))
                .thenReturn(Optional.of(feePaymentEntity));
            PaymentCallbackStrategy strategy = mock(PaymentCallbackStrategy.class);
            when(paymentCallbackStrategyFactory.getStrategy(CLAIM)).thenReturn(strategy);

            // When
            underTest.processPaymentResponse(paymentStatusCallback);

            // Then
            verify(strategy).handle(paymentStatusCallback, feePaymentEntity);
            verify(feePaymentRepository).save(feePaymentEntity);
        }

        @Test
        void shouldSkipStrategyAndStillSaveWhenNoStrategyRegisteredForJourneyId() {
            // Given
            String requestReference = UUID.randomUUID().toString();
            Payment payment = Payment.builder().paymentReference(UUID.randomUUID().toString()).build();
            PaymentStatusCallback paymentStatusCallback = PaymentStatusCallback.builder()
                .serviceRequestReference(requestReference).serviceRequestStatus(PaymentStatus.PAID.getValue())
                .payment(payment).build();
            FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.PAID).serviceRequestReference(requestReference)
                .paymentCallbackHandlerType(CLAIM).build();
            when(feePaymentRepository.findByServiceRequestReference(requestReference))
                .thenReturn(Optional.of(feePaymentEntity));
            when(paymentCallbackStrategyFactory.getStrategy(CLAIM)).thenReturn(null);

            // When
            underTest.processPaymentResponse(paymentStatusCallback);

            // Then
            verify(feePaymentRepository).save(feePaymentEntity);
        }
    }

    @Nested
    @DisplayName("Save new fee payment")
    class SaveNewFeePaymentTests {

        @Test
        void shouldSaveNewFeePaymentWithExpectedFields() throws IOException {
            // Given
            FeeDetails feeDetails = createFeeDetails();
            FeesAndPayTaskData feesAndPayTaskData = createFeesAndPayTaskData(feeDetails);
            String asString = objectMapper.writeValueAsString(feesAndPayTaskData);
            ClaimEntity claimEntity = new ClaimEntity();

            // When
            underTest.saveNewFeePayment(asString, feesAndPayTaskData, claimEntity, SERVICE_REQUEST_REFERENCE);

            // Then
            ArgumentCaptor<FeePaymentEntity> captor = ArgumentCaptor.forClass(FeePaymentEntity.class);
            verify(feePaymentRepository).save(captor.capture());
            FeePaymentEntity saved = captor.getValue();

            assertThat(saved.getClaim()).isSameAs(claimEntity);
            assertThat(saved.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST_REFERENCE);
            assertThat(saved.getAmount()).isEqualByComparingTo(CALCULATED_AMOUNT);
        }

        @Test
        void shouldSaveTaskDataAndJourneyIdWhenSavingNewFeePayment() throws Exception {
            // Given
            FeeDetails feeDetails = createFeeDetails();
            FeesAndPayTaskData feesAndPayTaskData = createFeesAndPayTaskData(feeDetails);
            String taskDataJson = objectMapper.writeValueAsString(feesAndPayTaskData);
            ClaimEntity claimEntity = new ClaimEntity();

            // When
            underTest.saveNewFeePayment(taskDataJson, feesAndPayTaskData, claimEntity, SERVICE_REQUEST_REFERENCE);

            // Then
            ArgumentCaptor<FeePaymentEntity> captor = ArgumentCaptor.forClass(FeePaymentEntity.class);
            verify(feePaymentRepository).save(captor.capture());
            FeePaymentEntity saved = captor.getValue();

            assertThat(saved.getTaskData()).isEqualTo(taskDataJson);
            assertThat(saved.getPaymentCallbackHandlerType()).isEqualTo(CLAIM);
            assertThat(saved.getParty()).isNull();
        }

    }

    @Nested
    @DisplayName("Create payment request")
    class CreatePaymentRequest {

        @Test
        void shouldCreatePaymentRequest() {
            // Given
            final String serviceRequestReference = "SR-1234";

            final BigDecimal expectedAmount = new BigDecimal("10.99");
            final String expectedLanguage = "some language";
            final String expectedReturnUrl = "some return URL";

            final String expectedPaymentReference = "some payment reference";
            final String expectedPaymentStatus = "some payment status";
            final String expectedNextUrl = "some next url";

            CreateCardPaymentRequest cardPaymentRequest = CreateCardPaymentRequest.builder()
                .amount(expectedAmount)
                .language(expectedLanguage)
                .returnUrl(expectedReturnUrl)
                .build();

            CardPaymentServiceRequestResponse paymentServiceResponse = CardPaymentServiceRequestResponse.builder()
                .paymentReference(expectedPaymentReference)
                .status(expectedPaymentStatus)
                .nextUrl(expectedNextUrl)
                .build();

            when(paymentsClient.createGovPayCardPaymentRequest(anyString(),
                                                               anyString(),
                                                               any(CardPaymentServiceRequestDTO.class)))
                .thenReturn(paymentServiceResponse);

            when(feePaymentRepository.findByServiceRequestReference(serviceRequestReference))
                .thenReturn(Optional.of(mock(FeePaymentEntity.class)));

            // When
            CreateCardPaymentResponse cardPaymentResponse = underTest.createPaymentRequest(
                serviceRequestReference,
                cardPaymentRequest
            );

            // Then
            verify(paymentsClient).createGovPayCardPaymentRequest(eq(serviceRequestReference),
                                                                  eq(SYSTEM_TOKEN),
                                                                  cardPaymentRequestCaptor.capture());

            CardPaymentServiceRequestDTO cardPaymentRequestDto = cardPaymentRequestCaptor.getValue();

            assertThat(cardPaymentRequestDto.getAmount()).isEqualTo(expectedAmount);
            assertThat(cardPaymentRequestDto.getLanguage()).isEqualTo(expectedLanguage);
            assertThat(cardPaymentRequestDto.getReturnUrl()).isEqualTo(expectedReturnUrl);

            assertThat(cardPaymentResponse.getPaymentReference()).isEqualTo(expectedPaymentReference);
            assertThat(cardPaymentResponse.getStatus()).isEqualTo(expectedPaymentStatus);
            assertThat(cardPaymentResponse.getNextUrl()).isEqualTo(expectedNextUrl);
        }

        @Test
        void shouldThrowExceptionCreatingPaymentRequestForUnknownServiceRequest() {
            // Given
            String serviceRequestReference = "SR-1234";
            CreateCardPaymentRequest cardPaymentRequest = mock(CreateCardPaymentRequest.class);

            when(feePaymentRepository.findByServiceRequestReference(serviceRequestReference))
                .thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> underTest.createPaymentRequest(
                serviceRequestReference,
                cardPaymentRequest
            ));

            // Then
            assertThat(throwable).isInstanceOf(FeePaymentNotFoundException.class);
        }

        @ParameterizedTest
        @EnumSource(PaymentStatus.class)
        void shouldThrowExceptionIfServiceRequestAlreadyHasAPaymentStatus(PaymentStatus paymentStatus) {
            // Given
            String serviceRequestReference = "SR-1234";
            CreateCardPaymentRequest cardPaymentRequest = mock(CreateCardPaymentRequest.class);

            FeePaymentEntity feePaymentEntity = mock(FeePaymentEntity.class);
            when(feePaymentRepository.findByServiceRequestReference(serviceRequestReference))
                .thenReturn(Optional.of(feePaymentEntity));
            when(feePaymentEntity.getPaymentStatus()).thenReturn(paymentStatus);

            // When
            Throwable throwable = catchThrowable(() -> underTest.createPaymentRequest(
                serviceRequestReference,
                cardPaymentRequest
            ));

            // Then
            assertThat(throwable).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Get payment status")
    class GetPaymentStatus {

        @Test
        void shouldGetPaymentStatus() {
            // Given
            String paymentReference = "CP-123";
            String expectedStatus = "some status";

            PaymentDto paymentDto = PaymentDto.builder()
                .status(expectedStatus)
                .build();

            when(paymentsClient.getGovPayCardPaymentStatus(paymentReference, SYSTEM_TOKEN)).thenReturn(paymentDto);

            // When
            CardPaymentStatusResponse paymentStatusResponse = underTest.getPaymentStatus(paymentReference);

            // Then
            assertThat(paymentStatusResponse.getStatus()).isEqualTo(expectedStatus);

        }

    }

    private void stubPcsCaseEntity() {
        ClaimPartyEntity claimPartyEntity = claimPartyEntity();
        PcsCaseEntity pcsCaseEntity = setupPcsCase(claimPartyEntity);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
    }

    private void paymentsClientDependencies(FeeDetails feeDetails) {
        FeeDto mappedFee = createFeeDto(feeDetails);
        CasePaymentRequestDto casePaymentRequestDto = createCasePaymentRequestDto();
        when(paymentRequestMapper.toFeeDto(feeDetails, VOLUME)).thenReturn(mappedFee);
        when(paymentRequestMapper.toCasePaymentRequest(RESPONSIBLE_PARTY))
            .thenReturn(casePaymentRequestDto);
    }

    private PcsCaseEntity setupPcsCase(ClaimPartyEntity claimPartyEntity) {
        ClaimEntity claimEntity = ClaimEntity.builder()
                .claimParties(List.of(claimPartyEntity)).build();
        return PcsCaseEntity.builder()
            .claims(List.of(claimEntity)).build();
    }

    private ClaimPartyEntity claimPartyEntity() {
        ClaimPartyEntity claimPartyEntity = mock(ClaimPartyEntity.class);
        PartyEntity partyEntity = mock(PartyEntity.class);
        lenient().when(partyEntity.getOrgName()).thenReturn(RESPONSIBLE_PARTY);
        lenient().when(claimPartyEntity.getParty()).thenReturn(partyEntity);
        return claimPartyEntity;
    }

    private CasePaymentRequestDto createCasePaymentRequestDto() {
        return CasePaymentRequestDto.builder().action("payment").responsibleParty(RESPONSIBLE_PARTY).build();
    }

    private PaymentServiceResponse createPaymentServiceResponse() {
        return PaymentServiceResponse.builder().serviceRequestReference(SERVICE_REQUEST_REFERENCE).build();
    }

    private FeeDto createFeeDto(FeeDetails feeDetails) {
        return new PaymentRequestMapper().toFeeDto(feeDetails, VOLUME);
    }

    private void assertServiceRequestCreation(FeesAndPayTaskData feesAndPayTaskData,
                                              PaymentServiceResponse result) {
        assertThat(result).isNotNull();
        assertThat(result.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST_REFERENCE);

        verify(paymentsClient).createServiceRequest(eq(SYSTEM_TOKEN), createServiceRequestCaptor.capture());
        CreateServiceRequestDTO sent = createServiceRequestCaptor.getValue();

        assertCreateServiceRequestDTO(feesAndPayTaskData, sent);
    }

    private void assertCreateServiceRequestDTO(FeesAndPayTaskData feesAndPayTaskData, CreateServiceRequestDTO sent) {
        assertThat(sent.getCallBackUrl()).isEqualTo(CALLBACK_URL);
        assertThat(sent.getHmctsOrgId()).isEqualTo(HMCTS_ORG_ID);
        assertThat(sent.getCaseReference()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(sent.getCcdCaseNumber()).isEqualTo(CCD_CASE_NUMBER);
        assertThat(sent.getFees()).isNotNull();
        assertThat(sent.getFees()).hasSize(1);
        assertThat(sent.getFees()[0]).isEqualTo(createFeeDto(feesAndPayTaskData.getFeeDetails()));
    }

    private static <T> void setPrivateField(T object, String fieldName, Object value) {
        try {
            var field = PaymentService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set private field", e);
        }
    }

    private FeesAndPayTaskData createFeesAndPayTaskData(FeeDetails feeDetails) {
        return FeesAndPayTaskData.builder()
            .caseReference(222)
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .feeDetails(feeDetails)
            .caseReference(CASE_REFERENCE)
            .volume(VOLUME)
            .responsiblePartyId(RESPONSIBLE_PARTY_ID)
            .paymentCallbackHandlerType(CLAIM)
            .build();
    }

    private FeeDetails createFeeDetails() {
        return FeeDetails.builder().feeAmount(CALCULATED_AMOUNT)
            .code("FEE123")
            .build();
    }

    private void stubResponsibleParty() {
        PartyEntity responsiblePartyEntity = mock(PartyEntity.class);
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE))
            .thenReturn(responsiblePartyEntity);
        when(partyService.getPartyName(responsiblePartyEntity)).thenReturn(RESPONSIBLE_PARTY);
    }

}
