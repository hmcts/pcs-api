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
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeGenerationService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class AccessCodeGenerationComponentTest {

    private AccessCodeGenerationComponent accessCodeGenerationComponent;

    @Mock
    private AccessCodeGenerationService accessCodeGenerationService;

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
            accessCodeGenerationService,
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
    @DisplayName("Should execute task and call AccessCodeService with case reference")
    void shouldExecuteTaskAndCallService() {
        //Given
        String caseReference = "123";
        AccessCodeTaskData data = AccessCodeTaskData.builder()
            .caseReference(caseReference)
            .build();

        when(taskInstance.getData()).thenReturn(data);
        CustomTask<AccessCodeTaskData> task = accessCodeGenerationComponent.accessCodeGenerationTask();

        //When
        CompletionHandler<AccessCodeTaskData> result = task.execute(taskInstance, executionContext);

        //Then
        verify(accessCodeGenerationService).createAccessCodesForParties(caseReference);

        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    @DisplayName("Should rethrow exception when service fails")
    void shouldRetryOnFailure() {
        //Given
        String caseReference = "999";
        AccessCodeTaskData data = AccessCodeTaskData.builder()
            .caseReference(caseReference)
            .build();

        when(taskInstance.getData()).thenReturn(data);

        when(executionContext.getExecution()).thenReturn(execution);

        doThrow(mock(RuntimeException.class))
            .when(accessCodeGenerationService)
            .createAccessCodesForParties(caseReference);

        //When
        CustomTask<AccessCodeTaskData> task = accessCodeGenerationComponent.accessCodeGenerationTask();

        //Then
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        verify(accessCodeGenerationService).createAccessCodesForParties(caseReference);
    }

}
