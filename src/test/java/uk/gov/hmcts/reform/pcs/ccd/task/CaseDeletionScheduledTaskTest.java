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
import uk.gov.hmcts.reform.pcs.exception.DocumentDeletionException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseDeletionScheduledTaskTest {

    private final int discardAfterDays = 30;

    private final Long case1 = 1L;
    private final Long case2 = 2L;

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
    class PerformCcdCaseDeletionEventsTests {

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
            verify(ccdCaseDataDeletionService).markCaseForDeletion(case2);
            verify(ccdCaseDataDeletionService).confirmCaseDisposal(case2);
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
        void shouldCallMarkCaseForDeletionBeforeConfirmCaseDisposal() {
            // Given
            List<Long> caseRefs = new ArrayList<>();
            caseRefs.add(case1);
            when(ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays))
                    .thenReturn(caseRefs);
            // When
            underTest.runSweep();

            // Then
            InOrder inOrder = inOrder(ccdCaseDataDeletionService);
            inOrder.verify(ccdCaseDataDeletionService).markCaseForDeletion(case1);
            inOrder.verify(ccdCaseDataDeletionService).confirmCaseDisposal(case1);
        }

        @Test
        void shouldAbortWhenEventThrowsCcdCaseNotFoundException() {
            // Given
            List<Long> caseRefs = new ArrayList<>();
            caseRefs.add(case1);
            when(ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays))
                    .thenReturn(caseRefs);

            doThrow(new CcdCaseNotFoundException(case1))
                    .when(ccdCaseDataDeletionService).markCaseForDeletion(case1);

            // When
            assertDoesNotThrow(() -> underTest.runSweep());

            // Then
            verify(ccdCaseDataDeletionService).markCaseForDeletion(case1);
            verify(ccdCaseDataDeletionService, never()).confirmCaseDisposal(case1);
        }

        @Test
        void shouldAbortIfFeignExceptionThrownFromEvent() {
            // Given
            when(ccdCaseDataDeletionService.findExpiredDraftCases(discardAfterDays))
                    .thenReturn(List.of(case1));
            FeignException feignException = mock(FeignException.class);
            doThrow(feignException)
                    .when(ccdCaseDataDeletionService).confirmCaseDisposal(case1);
            // When
            try {
                underTest.runSweep();
            } catch (FeignException e) {
                // Expected exception
            }
            // Then
            verify(ccdCaseDataDeletionService).markCaseForDeletion(case1);
            verify(ccdCaseDataDeletionService).confirmCaseDisposal(case1);
        }
    }

    @Nested
    class DeleteCasesFromDecentralisedSchemasTests {
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
            verify(caseDeletionService).deleteCaseData(case1);
            verify(caseDeletionService).deleteDocuments(case2);
            verify(caseDeletionService).deleteCaseData(case2);
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
        void shouldStopDeletionWhenExceptionThrownWhileDeletingDocuments() {
            // Given
            List<Long> caseRefs = List.of(case1);
            when(ccdCaseDataDeletionService.findExpiredDraftCasesInDraftDiscardedState())
                    .thenReturn(caseRefs);

            doThrow(new DocumentDeletionException(case1))
                    .when(caseDeletionService).deleteDocuments(case1);

            // When
            underTest.runSweep();

            // Then
            verify(caseDeletionService).deleteDocuments(case1);
            verify(caseDeletionService, never()).deleteCaseData(case1);
        }

        @Test
        void shouldContinueToNextCaseIfExceptionThrownWhileDeletingCurrentCaseDocuments() {
            // Given
            List<Long> caseRefs = List.of(case1, case2);
            when(ccdCaseDataDeletionService.findExpiredDraftCasesInDraftDiscardedState())
                    .thenReturn(caseRefs);

            doThrow(new DocumentDeletionException(case1))
                    .when(caseDeletionService).deleteDocuments(case1);

            // When
            underTest.runSweep();

            // Then
            verify(caseDeletionService).deleteDocuments(case1);
            verify(caseDeletionService, never()).deleteCaseData(case1);
            verify(caseDeletionService).deleteDocuments(case2);
            verify(caseDeletionService).deleteCaseData(case2);
        }
    }
}
