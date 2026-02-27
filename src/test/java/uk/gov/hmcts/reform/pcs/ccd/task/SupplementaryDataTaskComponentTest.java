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
import uk.gov.hmcts.reform.pcs.ccd.model.CaseReferenceTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.CcdSupplementaryDataService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.SupplementaryDataTaskComponent.SUPPLEMENTARY_DATA_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class SupplementaryDataTaskComponentTest {

    private SupplementaryDataTaskComponent supplementaryDataTaskComponent;

    @Mock
    private CcdSupplementaryDataService ccdSupplementaryDataService;

    @Mock
    private TaskInstance<CaseReferenceTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Execution execution;

    private final Duration backoffDelay = Duration.ofSeconds(3);
    private final int maxRetries = 5;

    @BeforeEach
    void setUp() {
        supplementaryDataTaskComponent = new SupplementaryDataTaskComponent(
            ccdSupplementaryDataService,
            maxRetries,
            backoffDelay
        );
    }

    @Test
    @DisplayName("Should create task descriptor with correct name and type")
    void shouldCreateTaskDescriptorWithCorrectNameAndType() {
        assertThat(SUPPLEMENTARY_DATA_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("supplementary-data-task");
        assertThat(SUPPLEMENTARY_DATA_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(CaseReferenceTaskData.class);
    }

    @Test
    @DisplayName("Should execute task and call CcdSupplementaryDataService with case reference")
    void shouldExecuteTaskAndCallService() {
        // Given
        String caseReference = "123";
        CaseReferenceTaskData data = CaseReferenceTaskData.builder()
            .caseReference(caseReference)
            .build();

        when(taskInstance.getData()).thenReturn(data);
        CustomTask<CaseReferenceTaskData> task = supplementaryDataTaskComponent.supplementaryDataTask();

        // When
        CompletionHandler<CaseReferenceTaskData> result = task.execute(taskInstance, executionContext);

        // Then
        verify(ccdSupplementaryDataService).submitSupplementaryDataRequestToCcd(caseReference);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    @DisplayName("Should rethrow exception when service fails")
    void shouldRetryOnFailure() {
        // Given
        String caseReference = "999";
        CaseReferenceTaskData data = CaseReferenceTaskData.builder()
            .caseReference(caseReference)
            .build();

        when(taskInstance.getData()).thenReturn(data);
        when(executionContext.getExecution()).thenReturn(execution);

        doThrow(mock(RuntimeException.class))
            .when(ccdSupplementaryDataService)
            .submitSupplementaryDataRequestToCcd(caseReference);

        CustomTask<CaseReferenceTaskData> task = supplementaryDataTaskComponent.supplementaryDataTask();

        // When & Then
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        verify(ccdSupplementaryDataService).submitSupplementaryDataRequestToCcd(caseReference);
    }
}
