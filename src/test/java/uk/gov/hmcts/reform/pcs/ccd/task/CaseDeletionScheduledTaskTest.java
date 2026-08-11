package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseDeletionScheduledTaskTest {

    private final int discardAfterDays = 30;

    @Mock
    private CcdCaseDataDeletionService ccdCaseDataDeletionService;
    @Mock
    private CaseDeletionService caseDeletionService;

    private CaseDeletionScheduledTask underTest;

    @BeforeEach
    void setUp() {
        String validSchedule = "DAILY|00:00";
        underTest = new CaseDeletionScheduledTask(validSchedule, discardAfterDays, 5, 10, 20, 45, 2,
                ccdCaseDataDeletionService, caseDeletionService);
    }

    @Test
    void shouldCreateRecurringTaskBeanSuccessfully() {
        RecurringTask<Void> recurringTask = underTest.caseDeletionTask();
        assertThat(recurringTask).isNotNull();
    }

    @Nested
    class ExpiredCasesDeletionTests {

        @Test
        void shouldProcessCasesForDeletionIfListOfExpiredCasesNotEmpty() {
            // Given
            when(ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays))
                    .thenReturn(List.of(1L, 2L));

            // When
            underTest.runSweep();

            // Then
            verify(ccdCaseDataDeletionService).findExpiredDraftCases(discardAfterDays);
            verify(caseDeletionService).performCaseDeletionTasks(1L);
            verify(caseDeletionService).performCaseDeletionTasks(2L);
        }

        @Test
        void shouldHandleEmptyListOfExpiredCases() {
            // Given
            when(ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays))
                    .thenReturn(List.of());

            // When
            underTest.runSweep();

            // Then
            verify(ccdCaseDataDeletionService).findExpiredDraftCases(discardAfterDays);
            verifyNoInteractions(caseDeletionService);
        }

        @Test
        void shouldContinueProcessingWhenIndividualDeletionThrowsException() {
            // Given
            List<Long> caseRefs = new ArrayList<>();
            caseRefs.add(111L);
            caseRefs.add(222L);
            caseRefs.add(333L);
            when(ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays))
                    .thenReturn(caseRefs);

            doThrow(new RuntimeException("Database down or API failure"))
                    .when(caseDeletionService).performCaseDeletionTasks(111L);

            // When
            assertDoesNotThrow(() -> underTest.runSweep());

            // Then
            verify(caseDeletionService, times(3)).performCaseDeletionTasks(anyLong());
            verify(caseDeletionService).performCaseDeletionTasks(111L);
            verify(caseDeletionService).performCaseDeletionTasks(222L);
            verify(caseDeletionService).performCaseDeletionTasks(333L);
        }
    }

    @Nested
    class DiscardedCasesDeletionTests {
        @Test
        void shouldProcessCasesForCleanupIfListOfDiscardedCasesNotEmpty() {
            // Given
            when(ccdCaseDataDeletionService.findExpiredDraftCasesInDraftDiscardedState())
                    .thenReturn(List.of(1L, 2L));

            // When
            underTest.runSweep();

            // Then
            verify(ccdCaseDataDeletionService).findExpiredDraftCasesInDraftDiscardedState();
            verify(caseDeletionService).cleanupDiscardedDraftCases(1L);
            verify(caseDeletionService).cleanupDiscardedDraftCases(2L);
        }

        @Test
        void shouldHandleEmptyListOfDiscardedCases() {
            // Given
            when(ccdCaseDataDeletionService.findExpiredDraftCasesInDraftDiscardedState())
                    .thenReturn(List.of());

            // When
            underTest.runSweep();

            // Then
            verify(ccdCaseDataDeletionService).findExpiredDraftCasesInDraftDiscardedState();
            verifyNoInteractions(caseDeletionService);
        }

        @Test
        void shouldContinueProcessingWhenIndividualDeletionThrowsException() {
            // Given
            List<Long> caseRefs = List.of(111L, 222L, 333L);
            when(ccdCaseDataDeletionService.findExpiredDraftCasesInDraftDiscardedState())
                    .thenReturn(caseRefs);

            doThrow(new RuntimeException("Database down or API failure"))
                    .when(caseDeletionService).cleanupDiscardedDraftCases(111L);

            // When
            assertDoesNotThrow(() -> underTest.runSweep());

            // Then
            verify(caseDeletionService, times(3)).cleanupDiscardedDraftCases(anyLong());
            verify(caseDeletionService).cleanupDiscardedDraftCases(111L);
            verify(caseDeletionService).cleanupDiscardedDraftCases(222L);
            verify(caseDeletionService).cleanupDiscardedDraftCases(333L);
        }
    }
}
