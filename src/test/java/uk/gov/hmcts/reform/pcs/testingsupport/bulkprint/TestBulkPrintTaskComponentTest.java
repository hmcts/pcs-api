package uk.gov.hmcts.reform.pcs.testingsupport.bulkprint;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.ResolvedRecipient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestBulkPrintTaskComponentTest {

    @Mock
    private TestBulkPrintSender sender;

    @Test
    @SuppressWarnings("unchecked")
    void shouldResolveThenPostTheRequestedLetter() {
        TestBulkPrintTaskData data = new TestBulkPrintTaskData(1234567890123456L, List.of());
        ResolvedRecipient resolved = mock(ResolvedRecipient.class);
        when(sender.resolveClaimant(data)).thenReturn(resolved);
        TaskInstance<TestBulkPrintTaskData> instance = mock(TaskInstance.class);
        when(instance.getData()).thenReturn(data);

        var task = new TestBulkPrintTaskComponent(sender).testBulkPrintTask();
        task.execute(instance, mock(ExecutionContext.class));

        assertThat(task.getName()).isEqualTo(TestBulkPrintTaskComponent.TEST_BULK_PRINT_TASK_NAME);
        verify(sender).post(resolved);
    }
}
