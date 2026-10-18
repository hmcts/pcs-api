package uk.gov.hmcts.reform.pcs.enforcement.document;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EnforcementDocumentSchedulerTest {

    private static final UUID ENFORCEMENT_ID = UUID.fromString("0f0e0d0c-0b0a-0908-0706-050403020100");

    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private EnforcementDocumentScheduler underTest;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldScheduleTheWarrantApplicationAndRiskAssessmentOncePerEnforcement() {
        underTest.scheduleApplicationDocuments(ENFORCEMENT_ID, SelectEnforcementType.WARRANT);

        assertThat(scheduled(2)).extracting(instance -> instance.getTaskInstance().getId())
            .containsExactly(ENFORCEMENT_ID + "-WARRANT_OF_POSSESSION_APPLICATION",
                             ENFORCEMENT_ID + "-RISK_ASSESSMENT");
    }

    @Test
    void shouldScheduleTheSealedWritOnApproval() {
        underTest.scheduleApprovalDocument(ENFORCEMENT_ID, SelectEnforcementType.WRIT);

        SchedulableInstance<?> instance = scheduled(1).getFirst();
        assertThat(instance.getTaskInstance().getTaskName()).isEqualTo("enforcement-document-generation-task");
        assertThat(instance.getTaskInstance().getData())
            .isEqualTo(new EnforcementDocumentTaskData(ENFORCEMENT_ID, EnforcementOutput.WRIT_OF_POSSESSION));
    }

    @Test
    void shouldScheduleNewNoticesEveryTimeTheEvictionIsScheduled() {
        underTest.scheduleEvictionNotices(ENFORCEMENT_ID);
        underTest.scheduleEvictionNotices(ENFORCEMENT_ID);

        assertThat(scheduled(4)).extracting(instance -> instance.getTaskInstance().getId())
            .doesNotHaveDuplicates()
            .allMatch(id -> id.startsWith(ENFORCEMENT_ID + "-NOTICE_OF_"));
    }

    @Test
    void shouldWaitForTheTransactionToCommit() {
        TransactionSynchronizationManager.initSynchronization();

        underTest.scheduleApprovalDocument(ENFORCEMENT_ID, SelectEnforcementType.WARRANT);
        verifyNoInteractions(schedulerClient);

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        assertThat(scheduled(1)).hasSize(1);
    }

    @Test
    void shouldNotThrowWhenSchedulingFailsAfterTheCommit() {
        doThrow(new IllegalStateException("database down")).when(schedulerClient).scheduleIfNotExists(any());

        underTest.scheduleApplicationDocuments(ENFORCEMENT_ID, SelectEnforcementType.WARRANT);

        verify(schedulerClient, times(2)).scheduleIfNotExists(any());
    }

    @Test
    void shouldScheduleNothingWithoutAnEnforcementType() {
        underTest.scheduleApplicationDocuments(ENFORCEMENT_ID, null);
        underTest.scheduleApprovalDocument(ENFORCEMENT_ID, null);

        verifyNoInteractions(schedulerClient);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<SchedulableInstance<?>> scheduled(int count) {
        ArgumentCaptor<SchedulableInstance> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient, times(count)).scheduleIfNotExists(captor.capture());
        return (List) captor.getAllValues();
    }
}
