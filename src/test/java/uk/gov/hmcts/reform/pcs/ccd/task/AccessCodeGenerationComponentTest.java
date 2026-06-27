package uk.gov.hmcts.reform.pcs.ccd.task;


import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantAccessCodeService;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class AccessCodeGenerationComponentTest {

    private AccessCodeGenerationComponent accessCodeGenerationComponent;

    @Mock
    private DefendantAccessCodeService defendantAccessCodeService;

    @Mock
    private TaskInstance<AccessCodeTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Execution execution;

    private final Duration backoffDelay = Duration.ofSeconds(3);

    private final int maxRetries = 5;

    @BeforeEach
    void setUp() {
        accessCodeGenerationComponent = new AccessCodeGenerationComponent(
            defendantAccessCodeService,
            maxRetries,
            backoffDelay
        );
    }

    @Test
    @DisplayName("Should create task descriptor with correct name and type")
    void shouldCreateTaskDescriptorWithCorrectNameAndType() {
        assertThat(ACCESS_CODE_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("access-code-generation-task");
        assertThat(ACCESS_CODE_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(AccessCodeTaskData.class);
    }

    @Test
    @DisplayName("Should execute task and generate the access code for the task's defendant")
    void shouldExecuteTaskAndGenerateForDefendant() {
        //Given
        String caseReference = "1234567812345678";
        UUID defendantPartyId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AccessCodeTaskData data = AccessCodeTaskData.builder()
            .caseReference(caseReference)
            .defendantPartyId(defendantPartyId.toString())
            .build();

        when(taskInstance.getData()).thenReturn(data);
        when(executionContext.getExecution()).thenReturn(execution);
        CustomTask<AccessCodeTaskData> task = accessCodeGenerationComponent.accessCodeGenerationTask();

        //When
        CompletionHandler<AccessCodeTaskData> result = task.execute(taskInstance, executionContext);

        //Then
        verify(defendantAccessCodeService).generateForDefendant(1234567812345678L, defendantPartyId, false);

        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    @DisplayName("Stamps partyId onto MDC during execution and clears it afterwards")
    void stampsPartyIdOnMdcDuringExecution() {
        String caseReference = "1234567812345678";
        UUID defendantPartyId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AccessCodeTaskData data = AccessCodeTaskData.builder()
            .caseReference(caseReference)
            .defendantPartyId(defendantPartyId.toString())
            .build();

        when(taskInstance.getData()).thenReturn(data);
        when(executionContext.getExecution()).thenReturn(execution);

        String[] partyIdSeenDuringExecution = new String[1];
        doAnswer(invocation -> {
            partyIdSeenDuringExecution[0] = MDC.get("partyId");
            return null;
        }).when(defendantAccessCodeService).generateForDefendant(anyLong(), any(), anyBoolean());

        CustomTask<AccessCodeTaskData> task = accessCodeGenerationComponent.accessCodeGenerationTask();
        task.execute(taskInstance, executionContext);

        assertThat(partyIdSeenDuringExecution[0]).isEqualTo(defendantPartyId.toString());
        assertThat(MDC.get("partyId")).isNull();
    }

    @Test
    @DisplayName("Should rethrow exception when generation fails")
    void shouldRetryOnFailure() {
        //Given
        String caseReference = "1234567812345678";
        UUID defendantPartyId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AccessCodeTaskData data = AccessCodeTaskData.builder()
            .caseReference(caseReference)
            .defendantPartyId(defendantPartyId.toString())
            .build();

        when(taskInstance.getData()).thenReturn(data);
        when(executionContext.getExecution()).thenReturn(execution);

        doThrow(mock(RuntimeException.class))
            .when(defendantAccessCodeService)
            .generateForDefendant(eq(1234567812345678L), eq(defendantPartyId), anyBoolean());

        //When
        CustomTask<AccessCodeTaskData> task = accessCodeGenerationComponent.accessCodeGenerationTask();

        //Then
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        verify(defendantAccessCodeService).generateForDefendant(eq(1234567812345678L), eq(defendantPartyId),
                                                                anyBoolean());
    }

}
