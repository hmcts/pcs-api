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
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;

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
import static uk.gov.hmcts.reform.pcs.ccd.task.PendingCounterClaimIssuedNotificationTaskComponent.PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class PendingCounterClaimIssuedNotificationTaskComponentTest {

    private static final int MAX_RETRIES = 3;
    private static final Duration BACKOFF_DELAY = Duration.ofSeconds(10);

    @Mock
    private CounterClaimRepository counterClaimRepository;

    @Mock
    private DefendantResponseNotificationService defendantResponseNotificationService;

    @Mock
    private TaskInstance<CounterClaimStatusChangeTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    private PendingCounterClaimIssuedNotificationTaskComponent underTest;

    @BeforeEach
    void setUp() {
        underTest = new PendingCounterClaimIssuedNotificationTaskComponent(
            counterClaimRepository,
            defendantResponseNotificationService,
            MAX_RETRIES,
            BACKOFF_DELAY
        );
    }

    @Test
    @DisplayName("Should create task descriptor with correct name and type")
    void shouldCreateTaskDescriptorWithCorrectNameAndType() {
        assertThat(PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("pending-counter-claim-issued-task");
        assertThat(PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(CounterClaimStatusChangeTaskData.class);
    }

    @Test
    @DisplayName("Should send notification when defendant response found")
    void shouldSendNotificationWhenDefendantResponseFound() {
        UUID counterClaimId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        UUID defendantResponseId = UUID.randomUUID();

        CounterClaimStatusChangeTaskData taskData = CounterClaimStatusChangeTaskData.builder()
            .counterClaimId(counterClaimId)
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        PartyEntity defendant = mock(PartyEntity.class);
        when(defendant.getId()).thenReturn(defendantId);

        DefendantResponseEntity defendantResponse = mock(DefendantResponseEntity.class);
        when(defendantResponse.getId()).thenReturn(defendantResponseId);
        when(defendantResponse.getParty()).thenReturn(defendant);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getDefendantResponses()).thenReturn(List.of(defendantResponse));

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        when(counterClaim.getParty()).thenReturn(defendant);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));

        CustomTask<CounterClaimStatusChangeTaskData> task = underTest.pendingCounterClaimIssuedNotificationTask();
        CompletionHandler<CounterClaimStatusChangeTaskData> completionHandler =
            task.execute(taskInstance, executionContext);

        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(defendantResponseNotificationService).sendEmailNotificationForCounterclaim(defendantResponseId);
    }

    @Test
    @DisplayName("Should not send notification when defendant response not found")
    void shouldNotSendNotificationWhenDefendantResponseNotFound() {
        UUID counterClaimId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        UUID otherPartyId = UUID.randomUUID();

        CounterClaimStatusChangeTaskData taskData = CounterClaimStatusChangeTaskData.builder()
            .counterClaimId(counterClaimId)
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        PartyEntity defendant = mock(PartyEntity.class);
        when(defendant.getId()).thenReturn(defendantId);

        PartyEntity otherParty = mock(PartyEntity.class);
        when(otherParty.getId()).thenReturn(otherPartyId);

        DefendantResponseEntity defendantResponse = mock(DefendantResponseEntity.class);
        when(defendantResponse.getParty()).thenReturn(otherParty);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getDefendantResponses()).thenReturn(List.of(defendantResponse));

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        when(counterClaim.getParty()).thenReturn(defendant);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));

        CustomTask<CounterClaimStatusChangeTaskData> task = underTest.pendingCounterClaimIssuedNotificationTask();
        CompletionHandler<CounterClaimStatusChangeTaskData> completionHandler =
            task.execute(taskInstance, executionContext);

        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verifyNoInteractions(defendantResponseNotificationService);
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

        CustomTask<CounterClaimStatusChangeTaskData> task = underTest.pendingCounterClaimIssuedNotificationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Counter claim not found: " + counterClaimId);
    }
}
