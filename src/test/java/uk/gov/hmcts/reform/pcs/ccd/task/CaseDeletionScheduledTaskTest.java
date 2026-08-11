package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseNotFoundException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseDeletionScheduledTaskTest {

    private final int discardAfterDays = 30;

    private final Long case1 = 1L;
    private final Long case2 = 2L;
    private final Long case3 = 3L;

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
                    .thenReturn(List.of(case1, case2));

            // When
            underTest.runSweep();

            // Then
            verify(ccdCaseDataDeletionService).findExpiredDraftCases(discardAfterDays);
            verify(ccdCaseDataDeletionService).markCaseForDeletion(case1);
            verify(ccdCaseDataDeletionService).confirmCaseDisposal(case1);
            verify(caseDeletionService).deleteDocuments(case1);
            verify(caseDeletionService).deleteCase(case1);
            verify(ccdCaseDataDeletionService).markCaseForDeletion(case2);
            verify(ccdCaseDataDeletionService).confirmCaseDisposal(case2);
            verify(caseDeletionService).deleteDocuments(case2);
            verify(caseDeletionService).deleteCase(case2);
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
            verify(ccdCaseDataDeletionService).findExpiredDraftCasesInDraftDiscardedState();
            verifyNoInteractions(caseDeletionService);
        }

        @Test
        void shouldContinueProcessingWhenIndividualDeletionThrowsException() {
            // Given
            List<Long> caseRefs = new ArrayList<>();
            caseRefs.add(case1);
            caseRefs.add(case2);
            caseRefs.add(case3);
            when(ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays))
                    .thenReturn(caseRefs);

            doThrow(new CcdCaseNotFoundException(case1))
                    .when(ccdCaseDataDeletionService).markCaseForDeletion(case1);

            // When
            assertDoesNotThrow(() -> underTest.runSweep());

            // Then
            verify(ccdCaseDataDeletionService, times(3)).markCaseForDeletion(anyLong());
            verify(ccdCaseDataDeletionService, times(2)).confirmCaseDisposal(anyLong());
            verify(caseDeletionService, times(3)).deleteDocuments(anyLong());
            verify(caseDeletionService, times(3)).deleteCase(anyLong());
        }

        @Test
        void shouldCallMarkCaseForDeletionBeforeConfirmCaseDisposal() {
            // Given & When
            underTest.performCaseDeletionTasks(case1);

            // Then
            InOrder inOrder = inOrder(ccdCaseDataDeletionService);
            inOrder.verify(ccdCaseDataDeletionService).markCaseForDeletion(case1);
            inOrder.verify(ccdCaseDataDeletionService).confirmCaseDisposal(case1);
        }

        @Test
        void shouldContinueWithDeletionWhenCcdCaseNotFoundExceptionThrownFromMarkCaseForDisposal() {
            // Given
            doThrow(new CcdCaseNotFoundException(case1))
                    .when(ccdCaseDataDeletionService).markCaseForDeletion(case1);

            // When
            underTest.performCaseDeletionTasks(case1);

            // Then
            verify(ccdCaseDataDeletionService).markCaseForDeletion(case1);
            verify(ccdCaseDataDeletionService, never()).confirmCaseDisposal(case1);
            verify(caseDeletionService).deleteDocuments(case1);
            verify(caseDeletionService).deleteCase(case1);
        }

        @Test
        void shouldAbortCaseDeletionIfFeignExceptionThrownFromMarkCaseForDisposal() {
            // Given
            when(ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays))
                    .thenReturn(List.of(case1));
            FeignException feignException = mock(FeignException.class);
            doThrow(feignException)
                    .when(ccdCaseDataDeletionService).markCaseForDeletion(case1);
            // When
            try {
                underTest.runSweep();
            } catch (FeignException e) {
                // Expected exception
            }
            // Then
            verify(ccdCaseDataDeletionService).markCaseForDeletion(case1);
            verify(ccdCaseDataDeletionService, never()).confirmCaseDisposal(case1);
            verify(caseDeletionService, never()).deleteDocuments(case1);
            verify(caseDeletionService, never()).deleteCase(case1);
        }
    }

    @Nested
    class DiscardedCasesDeletionTests {
        @Test
        void shouldProcessCasesForCleanupIfListOfDiscardedCasesNotEmpty() {
            // Given
            when(ccdCaseDataDeletionService.findExpiredDraftCasesInDraftDiscardedState())
                    .thenReturn(List.of(case1, case2));

            // When
            underTest.runSweep();

            // Then
            verify(ccdCaseDataDeletionService).findExpiredDraftCasesInDraftDiscardedState();
            verify(caseDeletionService).deleteDocuments(case1);
            verify(caseDeletionService).deleteCase(case1);
            verify(caseDeletionService).deleteDocuments(case2);
            verify(caseDeletionService).deleteCase(case2);
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

            doThrow(new RuntimeException("Database down"))
                    .when(caseDeletionService).deleteCase(111L);

            // When
            assertDoesNotThrow(() -> underTest.runSweep());

            // Then
            verify(caseDeletionService, times(3)).deleteCase(anyLong());
            verify(caseDeletionService).deleteCase(111L);
            verify(caseDeletionService).deleteCase(222L);
            verify(caseDeletionService).deleteCase(333L);
        }
    }
}
