package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.claim.ClaimWaTaskService;
import uk.gov.hmcts.reform.pcs.ccd.model.FeePaymentStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantAccessCodeService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormScheduler;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStateTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.system.SystemAction.CLAIM_ISSUED_PAYMENT;

@ExtendWith(MockitoExtension.class)
class MakeAClaimPaymentCallbackHandlerTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String CCD_CASE_NUMBER = "1111-2222-3333-4444";
    private static final String RESPONSIBLE_PARTY = "Claimant Org Ltd";
    private static final UUID RESPONSIBLE_PARTY_ID = UUID.randomUUID();

    @Mock
    private PartyService partyService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SystemEventExecutor systemEventExecutor;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ClaimFormScheduler claimFormScheduler;
    @Mock
    private DefendantAccessCodeService defendantAccessCodeService;
    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private ClaimWaTaskService claimWaTaskService;

    @InjectMocks
    private MakeAClaimPaymentCallbackHandler underTest;

    @ParameterizedTest
    @MethodSource("paymentStatus")
    void shouldHandle(PaymentStatus paymentStatus) throws Exception {
        // Given
        FeesAndPayTaskData taskData = buildTaskData();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).orgName(RESPONSIBLE_PARTY).build();
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE)).thenReturn(partyEntity);
        ClaimEntity claimEntity = new ClaimEntity();
        claimEntity.setId(UUID.randomUUID());
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(taskData.getCaseReference()).build();
        claimEntity.setPcsCase(pcsCase);
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().claim(claimEntity)
            .taskData(new ObjectMapper().writeValueAsString(taskData))
            .paymentStatus(paymentStatus).paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When
        underTest.handle(callback, feePaymentEntity);

        // Then
        assertThat(feePaymentEntity.getParty()).isSameAs(partyEntity);
        if (PaymentStatus.PAID == feePaymentEntity.getPaymentStatus()) {
            verify(systemEventExecutor).execute(anyLong(), any(UUID.class), any());
            ArgumentCaptor<TransactionDefinition> definitionCaptor =
                ArgumentCaptor.forClass(TransactionDefinition.class);
            verify(transactionManager).getTransaction(definitionCaptor.capture());
            assertThat(definitionCaptor.getValue().getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        } else {
            verifyNoInteractions(systemEventExecutor, transactionManager);
        }
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
        verifyNoInteractions(systemEventExecutor);
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
        verifyNoInteractions(systemEventExecutor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIssueCaseAndScheduleAllTasksWhenClaimNotYetIssued() {
        // Given
        UUID defendantOne = UUID.randomUUID();
        UUID defendantTwo = UUID.randomUUID();
        FeePaymentEntity claimFeePayment = FeePaymentEntity.builder().id(7)
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();

        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayments()).thenReturn(List.of(claimFeePayment));
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claim));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(CASE_REFERENCE))
            .thenReturn(List.of(defendantOne, defendantTwo));

        // When
        SystemEventResult result = underTest.apply(buildPaymentStateTaskData());

        // Then
        assertThat(result).isNotNull();
        verify(pcsCaseService).setCaseIssuedDate(CASE_REFERENCE);
        verify(claimFormScheduler).scheduleClaimFormGeneration(CASE_REFERENCE);
        verify(claimWaTaskService).createTasksForIssuedClaim(CASE_REFERENCE);
        // 2 access code tasks (one per defendant) + 1 claim-issued notification
        verify(schedulerClient, times(3)).scheduleIfNotExists(any(SchedulableInstance.class));
    }

    @Test
    void shouldNotRescheduleAnythingWhenClaimAlreadyIssued() {
        // Given
        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getClaimIssuedDate()).thenReturn(LocalDateTime.now());
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claim));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        SystemEventResult result = underTest.apply(buildPaymentStateTaskData());

        // Then - the state transition is still returned, but there are no duplicate side effects
        assertThat(result).isNotNull();
        verify(pcsCaseService, never()).setCaseIssuedDate(anyLong());
        verifyNoInteractions(claimFormScheduler, claimWaTaskService, schedulerClient,
                             defendantAccessCodeService);
    }

    @Test
    void shouldSkipClaimIssuedNotificationWhenNoFeePaymentExists() {
        // Given
        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayments()).thenReturn(List.of());
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claim));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(CASE_REFERENCE))
            .thenReturn(List.of());

        // When
        underTest.apply(buildPaymentStateTaskData());

        // Then
        verify(pcsCaseService).setCaseIssuedDate(CASE_REFERENCE);
        verifyNoInteractions(schedulerClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldScheduleNotificationForLatestClaimFeePaymentOnly() {
        // Given
        FeePaymentEntity olderClaimPayment = FeePaymentEntity.builder()
            .id(3).paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();
        FeePaymentEntity latestClaimPayment = FeePaymentEntity.builder()
            .id(9).paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();
        FeePaymentEntity otherTypePayment = FeePaymentEntity.builder()
            .id(20).paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE).build();

        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayments()).thenReturn(List.of(olderClaimPayment, latestClaimPayment, otherTypePayment));
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claim));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(CASE_REFERENCE))
            .thenReturn(List.of());

        // When
        underTest.apply(buildPaymentStateTaskData());

        // Then
        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());
        Object data = captor.getValue().getTaskInstance().getData();
        assertThat(data).isInstanceOf(FeePaymentStatusChangeTaskData.class);
        assertThat(((FeePaymentStatusChangeTaskData) data).getFeePaymentId()).isEqualTo(9);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldScheduleOneAccessCodeTaskPerDefendantWithDeterministicInstanceId() {
        // Given
        UUID defendantOne = UUID.randomUUID();
        UUID defendantTwo = UUID.randomUUID();
        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayments()).thenReturn(List.of());
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claim));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(CASE_REFERENCE))
            .thenReturn(List.of(defendantOne, defendantTwo));

        // When
        underTest.apply(buildPaymentStateTaskData());

        // Then
        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient, times(2)).scheduleIfNotExists(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(instance -> instance.getTaskInstance().getId())
            .containsExactlyInAnyOrder(CASE_REFERENCE + ":" + defendantOne,
                                       CASE_REFERENCE + ":" + defendantTwo);
    }

    @Test
    void shouldBuildPaymentStateTaskData() {
        // Given
        PaymentStatusCallback callback = PaymentStatusCallback.builder().payment(
            Payment.builder().paymentReference("RC-1234-5678").build()).build();
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .claim(ClaimEntity.builder().id(UUID.randomUUID()).build()).build();

        // When
        PaymentStateTaskData result = underTest.createPaymentStateTaskData(feePaymentEntity,
            CASE_REFERENCE, resumePossessionClaim, callback, CLAIM_ISSUED_PAYMENT);

        // Then
        assertThat(result.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(result.getEventId()).isEqualTo(resumePossessionClaim);
        assertThat(result.getSystemAction()).isEqualTo(CLAIM_ISSUED_PAYMENT);
        assertThat(result.getPaymentReference()).isEqualTo("RC-1234-5678");
        assertThat(result.getIdempotencyKey()).isNotNull();
    }

    @Test
    void shouldExecuteSystemEventForCaseReferenceInsideTransactionTemplate() {
        // Given
        SystemEventExecutionResult expectedResult = mock(SystemEventExecutionResult.class);
        when(systemEventExecutor.execute(eq(CASE_REFERENCE), any(UUID.class), any()))
            .thenReturn(expectedResult);

        // When
        underTest.executeEvent(buildPaymentStateTaskData());

        // Then
        verify(systemEventExecutor).execute(eq(CASE_REFERENCE), any(UUID.class), any());
        verify(transactionManager).getTransaction(any(TransactionDefinition.class));
    }

    private PaymentStateTaskData buildPaymentStateTaskData() {
        return PaymentStateTaskData.builder().caseReference(CASE_REFERENCE).eventId(resumePossessionClaim)
            .systemAction(CLAIM_ISSUED_PAYMENT).paymentReference("RC-1234-5678").idempotencyKey(UUID.randomUUID())
            .build();
    }

    private FeesAndPayTaskData buildTaskData() {
        return FeesAndPayTaskData.builder().caseReference(CASE_REFERENCE).ccdCaseNumber(CCD_CASE_NUMBER)
            .responsiblePartyId(RESPONSIBLE_PARTY_ID).paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM)
            .feeDetails(FeeDetails.builder().feeAmount(new BigDecimal("232.00")).code("FEE0412").build())
            .build();
    }

    private static Stream<PaymentStatus> paymentStatus() {
        return Stream.of(PaymentStatus.values());
    }

}
