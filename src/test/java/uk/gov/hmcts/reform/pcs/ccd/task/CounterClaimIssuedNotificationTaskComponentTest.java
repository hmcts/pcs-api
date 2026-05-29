package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.notify.service.PaymentNotificationService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.CounterClaimIssuedNotificationTaskComponent.COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class CounterClaimIssuedNotificationTaskComponentTest {

    private static final int MAX_RETRIES = 3;
    private static final Duration BACKOFF_DELAY = Duration.ofSeconds(10);

    @Mock
    private CounterClaimRepository counterClaimRepository;

    @Mock
    private PaymentNotificationService paymentNotificationService;

    @Mock
    private TaskInstance<CounterClaimStatusChangeTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    private CounterClaimIssuedNotificationTaskComponent underTest;

    @BeforeEach
    void setUp() {
        underTest = new CounterClaimIssuedNotificationTaskComponent(
            counterClaimRepository,
            paymentNotificationService,
            MAX_RETRIES,
            BACKOFF_DELAY
        );
    }

    @Test
    @DisplayName("Should create task descriptor with correct name and type")
    void shouldCreateTaskDescriptorWithCorrectNameAndType() {
        assertThat(COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("counter-claim-issued-task");
        assertThat(COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(CounterClaimStatusChangeTaskData.class);
    }

    @Test
    @DisplayName("Should send notification when fee is paid")
    void shouldSendNotificationWhenFeeIsPaid() {
        UUID counterClaimId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        UUID feePaymentId = UUID.randomUUID();

        CounterClaimStatusChangeTaskData taskData = CounterClaimStatusChangeTaskData.builder()
            .counterClaimId(counterClaimId)
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        PartyEntity defendant = mock(PartyEntity.class);
        when(defendant.getId()).thenReturn(defendantId);

        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getId()).thenReturn(feePaymentId);
        when(feePayment.getParty()).thenReturn(defendant);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.PAID);

        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayment()).thenReturn(feePayment);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getClaims()).thenReturn(List.of(claim));

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        when(counterClaim.getParty()).thenReturn(defendant);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));

        CustomTask<CounterClaimStatusChangeTaskData> task = underTest.counterClaimIssuedNotificationTask();
        CompletionHandler<CounterClaimStatusChangeTaskData> completionHandler =
            task.execute(taskInstance, executionContext);

        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(paymentNotificationService).sendCounterClaimPaymentSuccessNotification(feePaymentId);
    }

    @Test
    @DisplayName("Should not send notification when fee is not paid")
    void shouldNotSendNotificationWhenFeeIsNotPaid() {
        UUID counterClaimId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        UUID feePaymentId = UUID.randomUUID();

        CounterClaimStatusChangeTaskData taskData = CounterClaimStatusChangeTaskData.builder()
            .counterClaimId(counterClaimId)
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        PartyEntity defendant = mock(PartyEntity.class);
        when(defendant.getId()).thenReturn(defendantId);

        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getId()).thenReturn(feePaymentId);
        when(feePayment.getParty()).thenReturn(defendant);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.NOT_PAID);

        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayment()).thenReturn(feePayment);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getClaims()).thenReturn(List.of(claim));

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        when(counterClaim.getParty()).thenReturn(defendant);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));

        CustomTask<CounterClaimStatusChangeTaskData> task = underTest.counterClaimIssuedNotificationTask();
        CompletionHandler<CounterClaimStatusChangeTaskData> completionHandler =
            task.execute(taskInstance, executionContext);

        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verifyNoInteractions(paymentNotificationService);
    }

    @Test
    @DisplayName("Should throw exception when counter claim not found")
    void shouldThrowExceptionWhenCounterClaimNotFound() {
        UUID counterClaimId = UUID.randomUUID();
        CounterClaimStatusChangeTaskData taskData = CounterClaimStatusChangeTaskData.builder()
            .counterClaimId(counterClaimId)
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.empty());

        CustomTask<CounterClaimStatusChangeTaskData> task = underTest.counterClaimIssuedNotificationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Counter claim not found: " + counterClaimId);
    }

    @Test
    @DisplayName("Should throw exception when no fee payment found")
    void shouldThrowExceptionWhenNoFeePaymentFound() {
        UUID counterClaimId = UUID.randomUUID();

        CounterClaimStatusChangeTaskData taskData = CounterClaimStatusChangeTaskData.builder()
            .counterClaimId(counterClaimId)
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        PartyEntity defendant = mock(PartyEntity.class);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getClaims()).thenReturn(List.of());

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        when(counterClaim.getParty()).thenReturn(defendant);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);
        when(counterClaim.getId()).thenReturn(counterClaimId);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));

        CustomTask<CounterClaimStatusChangeTaskData> task = underTest.counterClaimIssuedNotificationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No fee payment found for counterclaim: " + counterClaimId);
    }
}
