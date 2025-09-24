package uk.gov.hmcts.reform.pcs.testingsupport.tasks;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HelloWorldTaskConfigTest {

    private Task<Void> task;
    private TaskInstance<Void> mockTaskInstance;
    private ExecutionContext mockExecutionContext;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        HelloWorldTaskConfig helloWorldTaskConfig = new HelloWorldTaskConfig();
        task = helloWorldTaskConfig.helloWorldTask();

        mockTaskInstance = mock(TaskInstance.class);
        when(mockTaskInstance.getId()).thenReturn("test-id");

        mockExecutionContext = mock(ExecutionContext.class);
    }

    @Test
    void testTaskFailsFirstThreeAttempts() {
        for (int i = 1; i <= 3; i++) {
            Throwable thrown = catchThrowable(() -> task.execute(mockTaskInstance, mockExecutionContext));

            assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated failure for demonstration");
        }
    }

    @Test
    void testTaskSucceedsAfterThreeFailures() {
        for (int i = 1; i <= 3; i++) {
            Throwable thrown = catchThrowable(() -> task.execute(mockTaskInstance, mockExecutionContext));
            assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated failure for demonstration");
        }

        assertThatCode(() -> task.execute(mockTaskInstance, mockExecutionContext))
            .doesNotThrowAnyException();
    }

    @Test
    void testCounterResetsAfterSuccess() {
        for (int i = 1; i <= 3; i++) {
            Throwable thrown = catchThrowable(() -> task.execute(mockTaskInstance, mockExecutionContext));
            assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated failure for demonstration");
        }

        assertThatCode(() -> task.execute(mockTaskInstance, mockExecutionContext))
            .doesNotThrowAnyException();

        Throwable thrownAfterReset = catchThrowable(() -> task.execute(mockTaskInstance, mockExecutionContext));
        assertThat(thrownAfterReset)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Simulated failure for demonstration");
    }
}
