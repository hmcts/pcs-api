package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.SystemEventAction;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativePartyLinkService;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NocAccessChangeTaskComponentTest {

    private static final int MAX_RETRIES = 3;
    private static final Duration BACKOFF_DELAY = Duration.ofSeconds(10);
    private static final Instant EXECUTION_TIME = Instant.parse("2026-09-03T09:30:00Z");
    private static final String TASK_INSTANCE_ID = "noc-task-1";
    private static final UUID EXPECTED_IDEMPOTENCY_KEY = UUID.nameUUIDFromBytes(
        ("noc-access-change-task:" + TASK_INSTANCE_ID + ":" + EXECUTION_TIME).getBytes(UTF_8)
    );

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
    private AtomicReference<SystemEventResult<?>> recordedEvent;

    @BeforeEach
    void setUp() {
        Execution execution = mock(Execution.class);
        when(taskInstance.getId()).thenReturn(TASK_INSTANCE_ID);
        when(executionContext.getExecution()).thenReturn(execution);
        when(execution.getExecutionTime()).thenReturn(EXECUTION_TIME);

        recordedEvent = new AtomicReference<>();
        doAnswer(invocation -> {
            SystemEventAction<?> action = invocation.getArgument(2);
            recordedEvent.set(action.execute());
            return null;
        }).when(systemEventExecutor).execute(anyLong(), any(UUID.class), any());

        nocAccessChangeTaskComponent = new NocAccessChangeTaskComponent(
            legalRepresentativePartyLinkService,
            systemEventExecutor,
            MAX_RETRIES,
            BACKOFF_DELAY
        );
    }

    @Test
    void nocAccessChangeTask() {
        // given
        String partyId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String email = "solicitor@example.com";
        NocAccessChangeTaskData taskData = NocAccessChangeTaskData.builder()
            .partyId(partyId)
            .organisationDetailsResponse(organisationDetailsResponse)
            .userId(userId)
            .email(email)
            .caseReference("1")
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        // when
        CustomTask<NocAccessChangeTaskData> task = nocAccessChangeTaskComponent.nocAccessChangeTask();
        CompletionHandler<NocAccessChangeTaskData> completionHandler =
            task.execute(taskInstance, executionContext);

        // then - access is granted through the case access groups, so the task only links the representative
        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(systemEventExecutor).execute(eq(1L), eq(EXPECTED_IDEMPOTENCY_KEY), any());
        verify(legalRepresentativePartyLinkService).linkLegalRepresentativeToParty(1L, partyId,
                                                                                   email,
                                                                                   organisationDetailsResponse);
        assertThat(recordedEvent.get())
            .extracting(
                SystemEventResult::eventId,
                SystemEventResult::eventName,
                SystemEventResult::summary,
                result -> result.state().isEmpty()
            )
            .containsExactly(
                "noticeOfChangeApplied",
                "Notice of change applied",
                "Notice of change by solicitor@example.com",
                true
            );
    }

    @Test
    void nocAccessChangeTask_shouldLogErrorAndThrowException_whenServiceFails() {
        // given
        String partyId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String email = "solicitor@example.com";
        NocAccessChangeTaskData taskData = NocAccessChangeTaskData.builder()
            .partyId(partyId)
            .organisationDetailsResponse(organisationDetailsResponse)
            .userId(userId)
            .email(email)
            .caseReference("1")
            .build();

        when(taskInstance.getData()).thenReturn(taskData);

        RuntimeException expectedException = new RuntimeException("Database error or service unavailable");
        doThrow(expectedException).when(legalRepresentativePartyLinkService)
            .linkLegalRepresentativeToParty(1L, partyId, email, organisationDetailsResponse);

        CustomTask<NocAccessChangeTaskData> task = nocAccessChangeTaskComponent.nocAccessChangeTask();

        // when & then - the failure propagates so db-scheduler can retry
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database error or service unavailable");

        verify(systemEventExecutor).execute(eq(1L), eq(EXPECTED_IDEMPOTENCY_KEY), any());
        verify(legalRepresentativePartyLinkService).linkLegalRepresentativeToParty(1L, partyId,
                                                                                   email,
                                                                                   organisationDetailsResponse);
    }
}
