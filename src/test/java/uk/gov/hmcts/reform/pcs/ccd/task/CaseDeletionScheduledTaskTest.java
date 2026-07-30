package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.model.DraftCasesToDiscard;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseDeletionScheduledTaskTest {

    private final int discardAfterDays = 30;
    private final long caseRef = 123456789L;
    private final long caseRef2 = 987654321L;

    @Mock
    private CcdCaseDataDeletionService ccdCaseDataDeletionService;
    @Mock
    private CaseDeletionService caseDeletionService;

    private CaseDeletionScheduledTask underTest;

    @BeforeEach
    void setUp() {
        String validSchedule = "DAILY|00:00";
        underTest = new CaseDeletionScheduledTask(validSchedule, discardAfterDays, ccdCaseDataDeletionService,
                caseDeletionService);
    }

    @Test
    void shouldCreateRecurringTaskBeanSuccessfully() {
        RecurringTask<Void> recurringTask = underTest.caseDeletionTask();
        assertThat(recurringTask).isNotNull();
    }

    @Test
    void shouldHandleEmptyListOfExpiredCases() throws Exception {
        // Given
        when(ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays))
                .thenReturn(List.of());

        // When
        runSweepViaReflection();

        // Then
        verify(ccdCaseDataDeletionService).findExpiredDraftCases(discardAfterDays);
        verifyNoMoreInteractions(ccdCaseDataDeletionService);
        verifyNoMoreInteractions(caseDeletionService);
    }

    @Test
    void shouldPerformCaseDeletionTasksForEachExpiredCase() throws Exception {
        // Given
        DraftCasesToDiscard case1 = DraftCasesToDiscard.builder().caseReference(caseRef).build();
        DraftCasesToDiscard case2 = DraftCasesToDiscard.builder().caseReference(caseRef2).build();

        when(ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays))
                .thenReturn(List.of(case1, case2));

        // When
        runSweepViaReflection();

        // Then
        verify(ccdCaseDataDeletionService).findExpiredDraftCases(discardAfterDays);
        verify(ccdCaseDataDeletionService).markCaseForDeletion(caseRef);
        verify(ccdCaseDataDeletionService).confirmCaseDisposal(caseRef);
        verify(caseDeletionService).deleteCase(caseRef);
        verify(ccdCaseDataDeletionService).markCaseForDeletion(caseRef2);
        verify(ccdCaseDataDeletionService).confirmCaseDisposal(caseRef2);
        verify(caseDeletionService).deleteCase(caseRef2);
        verifyNoMoreInteractions(ccdCaseDataDeletionService);
    }

    @Test
    void shouldCallMarkCaseForDeletionBeforeConfirmCaseDisposal() throws Exception {
        // Given
        DraftCasesToDiscard draftCase = DraftCasesToDiscard.builder().caseReference(caseRef).build();

        when(ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays))
                .thenReturn(List.of(draftCase));

        // When
        runSweepViaReflection();

        // Then
        InOrder inOrder = inOrder(ccdCaseDataDeletionService);
        inOrder.verify(ccdCaseDataDeletionService).markCaseForDeletion(caseRef);
        inOrder.verify(ccdCaseDataDeletionService).confirmCaseDisposal(caseRef);
    }

    private void runSweepViaReflection() throws Exception {
        Method method = CaseDeletionScheduledTask.class.getDeclaredMethod("runSweep");
        method.setAccessible(true);
        method.invoke(underTest);
    }
}
