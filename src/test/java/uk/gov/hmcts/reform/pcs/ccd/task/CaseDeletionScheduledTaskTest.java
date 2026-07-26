package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import uk.gov.hmcts.reform.pcs.ccd.model.DraftCasesToDiscard;
import uk.gov.hmcts.reform.pcs.ccd.service.CcdCaseDataService;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseDeletionScheduledTaskTest {

    @Mock
    private CcdCaseDataService ccdCaseDataService;

    private CaseDeletionScheduledTask underTest;

    @BeforeEach
    void setUp() {
        String validSchedule = "DAILY|00:00";
        int discardAfterDays = 7;
        underTest = new CaseDeletionScheduledTask(validSchedule, discardAfterDays, ccdCaseDataService);
    }

    @Test
    void shouldCreateRecurringTaskBeanSuccessfully() {
        RecurringTask<Void> task = underTest.caseDeletionTask();
        assertThat(task).isNotNull();
    }

    @Test
    void shouldPerformDeletionEventsForEachExpiredCase() throws Exception {
        // Given
        Long caseRef1 = 12345L;
        Long caseRef2 = 67890L;

        DraftCasesToDiscard case1 = DraftCasesToDiscard.builder().caseReference(caseRef1).build();
        DraftCasesToDiscard case2 = DraftCasesToDiscard.builder().caseReference(caseRef2).build();

        when(ccdCaseDataService.findExpiredDraftCases(7))
                .thenReturn(List.of(case1, case2));

        // When
        runSweepViaReflection();

        // Then
        verify(ccdCaseDataService).findExpiredDraftCases(7);
        verify(ccdCaseDataService).markCaseForDeletion(caseRef1);
        verify(ccdCaseDataService).confirmCaseDisposal(caseRef1);
        verify(ccdCaseDataService).markCaseForDeletion(caseRef2);
        verify(ccdCaseDataService).confirmCaseDisposal(caseRef2);
        verifyNoMoreInteractions(ccdCaseDataService);
    }

    @Test
    void shouldCallMarkCaseForDeletionBeforeConfirmCaseDisposal() throws Exception {
        // Given
        Long caseRef = 11111L;
        DraftCasesToDiscard draftCase = DraftCasesToDiscard.builder().caseReference(caseRef).build();

        when(ccdCaseDataService.findExpiredDraftCases(7))
                .thenReturn(List.of(draftCase));

        // When
        runSweepViaReflection();

        // Then - verify call order
        InOrder inOrder = inOrder(ccdCaseDataService);
        inOrder.verify(ccdCaseDataService).markCaseForDeletion(caseRef);
        inOrder.verify(ccdCaseDataService).confirmCaseDisposal(caseRef);
    }

    @Test
    void shouldHandleEmptyListOfExpiredCases() throws Exception {
        // Given
        when(ccdCaseDataService.findExpiredDraftCases(7))
                .thenReturn(List.of());

        // When
        runSweepViaReflection();

        // Then
        verify(ccdCaseDataService).findExpiredDraftCases(7);
        verifyNoMoreInteractions(ccdCaseDataService);
    }

    @Test
    void shouldCleanupMdcAfterRunSweep() throws Exception {
        // Given
        DraftCasesToDiscard draftCase = DraftCasesToDiscard.builder().caseReference(99999L).build();
        when(ccdCaseDataService.findExpiredDraftCases(7))
                .thenReturn(List.of(draftCase));

        // Ensure MDC is clean before test
        MDC.clear();

        // When
        runSweepViaReflection();

        // Then - MDC should be cleaned up after finally block
        assertThat(MDC.get("taskName")).isNull();
    }

    @Test
    void shouldCleanupMdcEvenWhenExceptionOccurs() {
        // Given
        DraftCasesToDiscard draftCase = DraftCasesToDiscard.builder().caseReference(99999L).build();
        when(ccdCaseDataService.findExpiredDraftCases(7))
                .thenReturn(List.of(draftCase));
        when(ccdCaseDataService.markCaseForDeletion(99999L))
                .thenThrow(new RuntimeException("Test exception"));

        MDC.clear();

        // When & Then - exception should not propagate; MDC should still be cleaned
        try {
            runSweepViaReflection();
        } catch (Exception e) {
            // Expected: the exception from markCaseForDeletion is wrapped and rethrown by reflection
        }

        // MDC should be cleaned up even though an exception occurred
        assertThat(MDC.get("taskName")).isNull();
    }

    // Helper method to invoke private runSweep() via reflection
    private void runSweepViaReflection() throws Exception {
        Method method = CaseDeletionScheduledTask.class.getDeclaredMethod("runSweep");
        method.setAccessible(true);
        method.invoke(underTest);
    }
}
