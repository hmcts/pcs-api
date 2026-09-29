package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.ActorAttribution;
import uk.gov.hmcts.ccd.sdk.SystemEventAction;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativePartyLinkService;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NocAccessChangeTaskComponentTest {

    private static final int MAX_RETRIES = 3;
    private static final Duration BACKOFF_DELAY = Duration.ofSeconds(10);
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("6f4a7b7e-1111-2222-3333-444455556666");

    @Mock
    private LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;

    @Mock
    private SystemEventExecutor systemEventExecutor;

    @Mock
    private TaskInstance<NocAccessChangeTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private OrganisationDetailsResponse organisationDetailsResponse;

    private NocAccessChangeTaskComponent nocAccessChangeTaskComponent;

    @BeforeEach
    void setUp() {
        nocAccessChangeTaskComponent = new NocAccessChangeTaskComponent(
            legalRepresentativePartyLinkService,
            systemEventExecutor,
            MAX_RETRIES,
            BACKOFF_DELAY
        );
    }

    @Test
    void recordsTheAccessChangeAsASystemEventAttributedToTheActingSolicitor() {
        // given
        NocAccessChangeTaskData taskData = taskData().build();
        when(taskInstance.getData()).thenReturn(taskData);
        when(systemEventExecutor.execute(anyLong(), any(ActorAttribution.class), any(UUID.class), any()))
            .thenReturn(executed());

        // when
        CustomTask<NocAccessChangeTaskData> task = nocAccessChangeTaskComponent.nocAccessChangeTask();
        CompletionHandler<NocAccessChangeTaskData> completionHandler =
            task.execute(taskInstance, executionContext);

        // then
        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        ArgumentCaptor<SystemEventAction> actionCaptor = ArgumentCaptor.forClass(SystemEventAction.class);
        verify(systemEventExecutor).execute(
            eq(1L),
            eq(new ActorAttribution(taskData.getUserId(), "Jane", "Doe")),
            eq(IDEMPOTENCY_KEY),
            actionCaptor.capture()
        );

        // the action links the representative and describes the event
        SystemEventResult result = actionCaptor.getValue().execute(null);
        verify(legalRepresentativePartyLinkService).linkLegalRepresentativeToParty(
            1L, taskData.getPartyId(), "solicitor@example.com", organisationDetailsResponse);
        assertThat(result.eventId()).isEqualTo("noticeOfChangeApplied");
        assertThat(result.eventName()).isEqualTo("Notice of change applied");
        assertThat(result.summary()).contains("Notice of change by solicitor@example.com");
        assertThat(result.state()).isEmpty();
    }

    @Test
    void recordsTheEventWithoutAnActorWhenTheSolicitorNamesAreBlank() {
        // given
        NocAccessChangeTaskData taskData = taskData().firstName(" ").lastName(null).build();
        when(taskInstance.getData()).thenReturn(taskData);
        when(systemEventExecutor.execute(anyLong(), any(UUID.class), any())).thenReturn(executed());

        // when
        nocAccessChangeTaskComponent.nocAccessChangeTask().execute(taskInstance, executionContext);

        // then
        verify(systemEventExecutor).execute(eq(1L), eq(IDEMPOTENCY_KEY), any());
        verify(systemEventExecutor, never())
            .execute(anyLong(), any(ActorAttribution.class), any(UUID.class), any());
    }

    @Test
    void omitsTheSummaryWhenTheEmailIsBlank() {
        // given
        NocAccessChangeTaskData taskData = taskData().email("  ").build();
        when(taskInstance.getData()).thenReturn(taskData);
        when(systemEventExecutor.execute(anyLong(), any(ActorAttribution.class), any(UUID.class), any()))
            .thenReturn(executed());

        // when
        nocAccessChangeTaskComponent.nocAccessChangeTask().execute(taskInstance, executionContext);

        // then
        ArgumentCaptor<SystemEventAction> actionCaptor = ArgumentCaptor.forClass(SystemEventAction.class);
        verify(systemEventExecutor).execute(anyLong(), any(ActorAttribution.class), any(UUID.class),
                                            actionCaptor.capture());
        assertThat(actionCaptor.getValue().execute(null).summary()).isEmpty();
    }

    @Test
    void propagatesTheFailureSoTheTaskRetries_withoutTouchingTheLink() {
        // given
        NocAccessChangeTaskData taskData = taskData().build();
        when(taskInstance.getData()).thenReturn(taskData);

        Execution execution = mock(Execution.class);
        when(executionContext.getExecution()).thenReturn(execution);

        doThrow(new RuntimeException("executor unavailable"))
            .when(systemEventExecutor)
            .execute(anyLong(), any(ActorAttribution.class), any(UUID.class), any());

        CustomTask<NocAccessChangeTaskData> task = nocAccessChangeTaskComponent.nocAccessChangeTask();

        // when / then
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("executor unavailable");

        verifyNoInteractions(legalRepresentativePartyLinkService);
    }

    @Test
    void completesWhenTheEventWasAlreadyRecorded() {
        // given - a retry after the transaction committed replays as a no-op
        NocAccessChangeTaskData taskData = taskData().build();
        when(taskInstance.getData()).thenReturn(taskData);
        when(systemEventExecutor.execute(anyLong(), any(ActorAttribution.class), any(UUID.class), any()))
            .thenReturn(new SystemEventExecutionResult(42L, SystemEventExecutionResult.Outcome.REPLAYED));

        // when
        CompletionHandler<NocAccessChangeTaskData> completionHandler =
            nocAccessChangeTaskComponent.nocAccessChangeTask().execute(taskInstance, executionContext);

        // then - the action was never invoked by the executor, so the link is untouched
        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verifyNoInteractions(legalRepresentativePartyLinkService);
    }

    private SystemEventExecutionResult executed() {
        return new SystemEventExecutionResult(42L, SystemEventExecutionResult.Outcome.EXECUTED);
    }

    private NocAccessChangeTaskData.NocAccessChangeTaskDataBuilder taskData() {
        return NocAccessChangeTaskData.builder()
            .partyId(UUID.randomUUID().toString())
            .organisationDetailsResponse(organisationDetailsResponse)
            .userId(UUID.randomUUID().toString())
            .email("solicitor@example.com")
            .firstName("Jane")
            .lastName("Doe")
            .caseReference("1")
            .eventIdempotencyKey(IDEMPOTENCY_KEY);
    }
}
