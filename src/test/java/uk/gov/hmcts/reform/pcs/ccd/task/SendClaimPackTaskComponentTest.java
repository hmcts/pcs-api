package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.ClaimPackSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SendClaimPackTaskComponentTest {

    @Mock
    private ClaimPackSender claimPackSender;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private TaskInstance<SendClaimPackTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    private SendClaimPackTaskComponent underTest;

    private static final long TEST_CASE_REFERENCE = 1234L;

    @BeforeEach
    void setUp() {
        underTest = new SendClaimPackTaskComponent(claimPackSender, pcsCaseService);
    }

    @Test
    void shouldCallClaimPackSender() {
        // Given
        SendClaimPackTaskData taskData = mock(SendClaimPackTaskData.class);
        when(taskData.getCaseReference()).thenReturn(TEST_CASE_REFERENCE);
        when(taskInstance.getData()).thenReturn(taskData);

        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        UUID uuid = UUID.randomUUID();
        when(pcsCaseEntity.getId()).thenReturn(uuid);

        // When
        underTest.sendClaimPackTask().execute(taskInstance, executionContext);

        // Then
        verify(claimPackSender).sendClaimPacks(uuid);
    }

    @Test
    void shouldRemoveTaskAfterSuccessfulExecution() {
        // Given
        SendClaimPackTaskData taskData = mock(SendClaimPackTaskData.class);
        when(taskData.getCaseReference()).thenReturn(TEST_CASE_REFERENCE);
        when(taskInstance.getData()).thenReturn(taskData);

        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        UUID uuid = UUID.randomUUID();
        when(pcsCaseEntity.getId()).thenReturn(uuid);

        // When
        CompletionHandler<SendClaimPackTaskData> completionHandler
            = underTest.sendClaimPackTask().execute(taskInstance, executionContext);

        // Then
        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }
}
