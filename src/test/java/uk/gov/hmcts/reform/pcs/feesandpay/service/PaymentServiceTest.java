package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.mapper.PaymentRequestMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestUpdate;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final long CASE_REFERENCE = 123L;
    private static final String CCD_CASE_NUMBER = "1111-2222-3333-4444";
    private static final int VOLUME = 2;
    private static final String RESPONSIBLE_PARTY = "Applicant";
    private static final String SYSTEM_TOKEN = "Bearer sys-token";
    private static final BigDecimal CALCULATED_AMOUNT = new BigDecimal("808.00");
    private static final String FEE_CODE = "FEE0412";
    private static final String FEE_VERSION = "4";
    private static final String SERVICE_REQUEST_REFERENCE = "SR-123";
    private static final String CALLBACK_URL = "https://etc:123/service-request-update";
    private static final String HMCTS_ORG_ID = "TEST_ORG";

    @Mock
    private PaymentsClient paymentsClient;
    @Mock
    private PaymentRequestMapper paymentRequestMapper;
    @Mock
    private IdamService idamService;
    @Mock
    private FeePaymentRepository feePaymentRepository;
    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private PaymentService underTest;

    @Captor
    private ArgumentCaptor<CreateServiceRequestDTO> createServiceRequestCaptor;

    @BeforeEach
    void setUp() {
        setPrivateField(underTest, "callbackUrl", CALLBACK_URL);
        setPrivateField(underTest, "hmctsOrgId", HMCTS_ORG_ID);
    }

    @Test
    void shouldCreateServiceRequestSuccessfully() {
        // Given
        FeeDetails feeDetails = Instancio.create(FeeDetails.class);
        paymentsClientDependencies(feeDetails);
        ClaimPartyEntity claimPartyEntity = claimPartyEntity();
        PcsCaseEntity pcsCaseEntity = setupPcsCase(claimPartyEntity);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        PaymentServiceResponse expectedResponse = createPaymentServiceResponse();
        when(paymentsClient.createServiceRequest(any(), any(CreateServiceRequestDTO.class)))
            .thenReturn(expectedResponse);

        // When
        PaymentServiceResponse result = underTest.createServiceRequest(String.valueOf(CASE_REFERENCE), CCD_CASE_NUMBER,
                                                                       feeDetails, VOLUME, RESPONSIBLE_PARTY);

        // Then
        assertServiceRequestCreation(result);
    }

    @Test
    void shouldCreateServiceRequest_NoPCSCase() {
        // Given
        FeeDetails feeDetails = Instancio.create(FeeDetails.class);
        when(pcsCaseService.loadCase(anyLong())).thenThrow(new CaseNotFoundException(222L));

        // When
        assertThatThrownBy(() -> underTest.createServiceRequest("222", CCD_CASE_NUMBER,
                                                                feeDetails, VOLUME, RESPONSIBLE_PARTY))
            .isInstanceOf(CaseNotFoundException.class);
    }

    @Test
    void shouldProcessPaymentResponse() {
        // Given
        String requestReference = UUID.randomUUID().toString();
        String paymentReference = UUID.randomUUID().toString();
        Payment payment = Payment.builder().paymentReference(paymentReference).build();
        ServiceRequestUpdate serviceRequestUpdate = ServiceRequestUpdate.builder()
            .serviceRequestReference(requestReference).serviceRequestStatus(PaymentStatus.PAID.getValue())
            .payment(payment)
            .build();
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.PAID)
            .requestReference(requestReference)
            .externalReference(paymentReference)
            .build();
        when(feePaymentRepository.findByRequestReference(requestReference)).thenReturn(Optional.of(feePaymentEntity));

        // When
        underTest.processPaymentResponse(serviceRequestUpdate);

        // Then
        verify(feePaymentRepository).findByRequestReference(requestReference);
        ArgumentCaptor<FeePaymentEntity> feePaymentCaptor = ArgumentCaptor.forClass(FeePaymentEntity.class);
        verify(feePaymentRepository).save(feePaymentCaptor.capture());
        FeePaymentEntity paymentEntity = feePaymentCaptor.getValue();

        assertThat(paymentEntity.getRequestReference()).isEqualTo(requestReference);
        assertThat(paymentEntity.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(paymentEntity.getExternalReference()).isEqualTo(paymentReference);
    }

    @Test
    void shouldNotUpdateFeePaymentWhenRequestReferenceNotFound() {
        // Given
        String requestReference = UUID.randomUUID().toString();
        ServiceRequestUpdate serviceRequestUpdate = ServiceRequestUpdate.builder()
            .serviceRequestReference(requestReference)
            .serviceRequestStatus(PaymentStatus.PAID.getValue())
            .payment(Payment.builder().paymentReference(UUID.randomUUID().toString()).build())
            .build();
        when(feePaymentRepository.findByRequestReference(requestReference)).thenReturn(Optional.empty());

        // When
        underTest.processPaymentResponse(serviceRequestUpdate);

        // Then
        verify(feePaymentRepository).findByRequestReference(requestReference);
        verify(feePaymentRepository, org.mockito.Mockito.never()).save(any(FeePaymentEntity.class));
    }

    @Test
    void shouldSaveNewFeePaymentWithExpectedFields() {
        // Given
        ClaimEntity claimEntity = new ClaimEntity();
        FeeDto feeDto = createFeeDto();

        // When
        underTest.saveNewFeePayment(String.valueOf(CASE_REFERENCE), claimEntity, feeDto, SERVICE_REQUEST_REFERENCE);

        // Then
        ArgumentCaptor<FeePaymentEntity> captor = ArgumentCaptor.forClass(FeePaymentEntity.class);
        verify(feePaymentRepository).save(captor.capture());
        FeePaymentEntity saved = captor.getValue();

        assertThat(saved.getClaim()).isSameAs(claimEntity);
        assertThat(saved.getRequestReference()).isEqualTo(SERVICE_REQUEST_REFERENCE);
        assertThat(saved.getAmount()).isEqualByComparingTo(CALCULATED_AMOUNT);
    }

    @Test
    void shouldPersistFeePaymentWhenCreatingServiceRequest() {
        // Given
        FeeDetails feeDetails = Instancio.create(FeeDetails.class);
        paymentsClientDependencies(feeDetails);
        PcsCaseEntity pcsCaseEntity = setupPcsCase(claimPartyEntity());
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(paymentsClient.createServiceRequest(any(), any(CreateServiceRequestDTO.class)))
            .thenReturn(createPaymentServiceResponse());

        // When
        underTest.createServiceRequest(String.valueOf(CASE_REFERENCE), CCD_CASE_NUMBER,
                                       feeDetails, VOLUME, RESPONSIBLE_PARTY);

        // Then
        ArgumentCaptor<FeePaymentEntity> captor = ArgumentCaptor.forClass(FeePaymentEntity.class);
        verify(feePaymentRepository).save(captor.capture());
        FeePaymentEntity saved = captor.getValue();

        assertThat(saved.getRequestReference()).isEqualTo(SERVICE_REQUEST_REFERENCE);
        assertThat(saved.getAmount()).isEqualByComparingTo(CALCULATED_AMOUNT);
        assertThat(saved.getClaim()).isSameAs(pcsCaseEntity.getClaims().getFirst());
    }

    private void paymentsClientDependencies(FeeDetails feeDetails) {
        FeeDto mappedFee = createFeeDto();
        CasePaymentRequestDto casePaymentRequestDto = createCasePaymentRequestDto();
        when(paymentRequestMapper.toFeeDto(feeDetails, VOLUME)).thenReturn(mappedFee);
        when(paymentRequestMapper.toCasePaymentRequest(RESPONSIBLE_PARTY))
            .thenReturn(casePaymentRequestDto);
        when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_TOKEN);
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

    private FeeDto createFeeDto() {
        return FeeDto.builder().calculatedAmount(CALCULATED_AMOUNT).code(FEE_CODE).version(FEE_VERSION)
            .volume(VOLUME).build();
    }

    private void assertServiceRequestCreation(PaymentServiceResponse result) {
        assertThat(result).isNotNull();
        assertThat(result.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST_REFERENCE);

        verify(paymentsClient).createServiceRequest(eq(SYSTEM_TOKEN), createServiceRequestCaptor.capture());
        CreateServiceRequestDTO sent = createServiceRequestCaptor.getValue();

        assertCreateServiceRequestDTO(sent);
    }

    private void assertCreateServiceRequestDTO(CreateServiceRequestDTO sent) {
        assertThat(sent.getCallBackUrl()).isEqualTo(CALLBACK_URL);
        assertThat(sent.getHmctsOrgId()).isEqualTo(HMCTS_ORG_ID);
        assertThat(sent.getCaseReference()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(sent.getCcdCaseNumber()).isEqualTo(CCD_CASE_NUMBER);
        assertThat(sent.getFees()).isNotNull();
        assertThat(sent.getFees()).hasSize(1);
        assertThat(sent.getFees()[0]).isEqualTo(createFeeDto());
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

}
