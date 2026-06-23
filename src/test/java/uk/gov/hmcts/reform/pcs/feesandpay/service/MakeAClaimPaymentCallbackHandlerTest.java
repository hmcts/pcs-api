package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MakeAClaimPaymentCallbackHandlerTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String CCD_CASE_NUMBER = "1111-2222-3333-4444";
    private static final String RESPONSIBLE_PARTY = "Claimant Org Ltd";
    private static final UUID RESPONSIBLE_PARTY_ID = UUID.randomUUID();

    @Mock
    private CcdPaymentStateUpdateService ccdPaymentStateUpdateService;
    @Mock
    private PartyService partyService;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private MakeAClaimPaymentCallbackHandler underTest;

    @BeforeEach
    void beforeEach() {
        ReflectionTestUtils.setField(underTest, "utcClock", Clock.systemUTC());
    }

    @Test
    void shouldSetPartyAllocateCaseManagementLocationSubmitPaymentSuccessAndIssueClaimWhenPaid() throws Exception {
        // Given
        FeesAndPayTaskData taskData = buildTaskData();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        String taskDataJson = new ObjectMapper().writeValueAsString(taskData);

        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).orgName(RESPONSIBLE_PARTY).build();
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE)).thenReturn(partyEntity);

        ClaimEntity claimEntity = new ClaimEntity();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(taskData.getCaseReference()).build();
        claimEntity.setPcsCase(pcsCase);
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().claim(claimEntity).taskData(taskDataJson)
            .paymentStatus(PaymentStatus.PAID).paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When
        underTest.handle(callback, feePaymentEntity);

        // Then
        assertThat(feePaymentEntity.getParty()).isSameAs(partyEntity);
        assertThat(claimEntity.getClaimIssuedDate()).isNotNull();
        var inOrder = inOrder(pcsCaseService, ccdPaymentStateUpdateService, claimRepository);
        inOrder.verify(pcsCaseService).allocateCaseManagementLocation(taskData.getCaseReference());
        inOrder.verify(ccdPaymentStateUpdateService).submitPaymentSuccess(taskData.getCaseReference());
        inOrder.verify(claimRepository).save(claimEntity);
    }

    @Test
    void shouldNotSubmitPaymentSuccessOrIssueClaimWhenCaseManagementLocationAllocationFails() throws Exception {
        // Given
        FeesAndPayTaskData taskData = buildTaskData();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        String taskDataJson = new ObjectMapper().writeValueAsString(taskData);

        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).orgName(RESPONSIBLE_PARTY).build();
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE)).thenReturn(partyEntity);

        RuntimeException expectedException = new RuntimeException("Lookup failed");
        doThrow(expectedException).when(pcsCaseService).allocateCaseManagementLocation(CASE_REFERENCE);

        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().taskData(taskDataJson)
            .paymentStatus(PaymentStatus.PAID).paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When
        Throwable throwable = catchThrowable(() -> underTest.handle(callback, feePaymentEntity));

        // Then
        assertThat(throwable).isSameAs(expectedException);
        verify(ccdPaymentStateUpdateService, never()).submitPaymentSuccess(CASE_REFERENCE);
        verifyNoInteractions(claimRepository);
    }

    @ParameterizedTest
    @MethodSource("nonPaidPaymentStatus")
    void shouldSetPartyAndNotAllocateCaseManagementLocationWhenPaymentIsNotPaid(PaymentStatus paymentStatus)
        throws Exception {
        // Given
        FeesAndPayTaskData taskData = buildTaskData();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        String taskDataJson = new ObjectMapper().writeValueAsString(taskData);

        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).orgName(RESPONSIBLE_PARTY).build();
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE)).thenReturn(partyEntity);

        ClaimEntity claimEntity = new ClaimEntity();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(taskData.getCaseReference()).build();
        claimEntity.setPcsCase(pcsCase);
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().claim(claimEntity).taskData(taskDataJson)
            .paymentStatus(paymentStatus).paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When
        underTest.handle(callback, feePaymentEntity);

        // Then
        assertThat(feePaymentEntity.getParty()).isSameAs(partyEntity);
        verifyNoInteractions(pcsCaseService);
        verifyNoInteractions(ccdPaymentStateUpdateService);
        verifyNoInteractions(claimRepository);
    }

    @Test
    void shouldThrowPaymentCallbackExceptionWhenTaskDataIsInvalidJson() throws Exception {
        // Given
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class)))
            .thenThrow(new JsonProcessingException("Invalid JSON") {});
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().claim(new ClaimEntity()).taskData("aasdfsdf{{")
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();

        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When / Then
        assertThatExceptionOfType(PaymentCallbackException.class)
            .isThrownBy(() -> underTest.handle(callback, feePaymentEntity))
            .withMessageContaining("Unable to process");
    }

    @Test
    void shouldPropagatePartyNotFoundExceptionWhenNoPartyMatchesResponsibleParty() throws Exception {
        // Given
        FeesAndPayTaskData taskData = buildTaskData();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        String taskDataJson = "some task json";

        PartyNotFoundException expectedException = mock(PartyNotFoundException.class);
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE))
            .thenThrow(expectedException);

        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().taskData(taskDataJson)
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();

        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When
        Throwable throwable = catchThrowable(() -> underTest.handle(callback, feePaymentEntity));

        // Then
        assertThat(throwable).isEqualTo(expectedException);
        verifyNoInteractions(pcsCaseService);
        verifyNoInteractions(ccdPaymentStateUpdateService);
        verifyNoInteractions(claimRepository);
    }

    private FeesAndPayTaskData buildTaskData() {
        return FeesAndPayTaskData.builder()
            .caseReference(CASE_REFERENCE)
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .responsiblePartyId(RESPONSIBLE_PARTY_ID)
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM)
            .feeDetails(FeeDetails.builder().feeAmount(new BigDecimal("232.00")).code("FEE0412").build())
            .build();
    }

    private static Stream<PaymentStatus> paymentStatus() {
        return Stream.of(PaymentStatus.values());
    }

    private static Stream<PaymentStatus> nonPaidPaymentStatus() {
        return paymentStatus().filter(paymentStatus -> PaymentStatus.PAID != paymentStatus);
    }

}
