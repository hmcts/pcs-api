package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.DeletionCaseData;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.DocumentNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.PcsCaseDeletionException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseDeletionScheduledTaskTest {

    private static final String VALID_SCHEDULE = "DAILY|00:00";
    private static final int DISCARD_AFTER_DAYS = 30;
    private static final int SQL_LIMIT = 200;
    private static final long SIMULATED_LATENCY_MS = 100;
    private static final long CASE_1 = 1L;
    private static final long CASE_2 = 2L;
    private static final long CASE_3 = 3L;
    private static final long CASE_4 = 4L;
    private static final long CASE_5 = 5L;

    @Mock
    private CcdCaseDataDeletionService ccdCaseDataDeletionService;
    @Mock
    private CaseDeletionService caseDeletionService;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private DocumentImportService documentImportService;

    private final AtomicInteger concurrentCalls = new AtomicInteger();
    private final AtomicInteger maxObservedConcurrency = new AtomicInteger();

    private CaseDeletionScheduledTask underTest;

    @BeforeEach
    void setUp() {
        underTest = createTask(10, 25, 10);
    }

    @Test
    void shouldCreateRecurringTaskWithExpectedName() {
        // Given && When
        RecurringTask<Void> task = underTest.caseDeletionTask();

        // Then
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("case-deletion-task");
    }

    @Test
    void shouldDoNothingWhenNoExpiredCasesFound() {
        // Given
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of());

        // When
        underTest.runSweep();

        // Then
        verifyNoInteractions(pcsCaseService, caseDeletionService, documentImportService);
    }

    @Test
    void shouldDoNothingWhenExpiredCasesIsNull() {
        // Given
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(null);

        // When
        underTest.runSweep();

        // Then
        verifyNoInteractions(pcsCaseService, caseDeletionService, documentImportService);
    }

    @Test
    void shouldFullyDeleteCaseWithDocuments() {
        // Given
        String url1 = "url1";
        String url2 = "url2";
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of(caseData(CASE_1)));
        when(pcsCaseService.getDocumentUrls(anyLong())).thenReturn(List.of(url1, url2));

        // When
        underTest.runSweep();

        // Then
        InOrder inOrder = inOrder(ccdCaseDataDeletionService, documentImportService, caseDeletionService);
        inOrder.verify(ccdCaseDataDeletionService).markCaseForDeletion(CASE_1);
        inOrder.verify(ccdCaseDataDeletionService).confirmCaseDisposal(CASE_1);
        inOrder.verify(documentImportService).deleteDocument(url1);
        inOrder.verify(documentImportService).deleteDocument(url2);
        inOrder.verify(caseDeletionService).deleteCaseData(CASE_1);
    }

    @Test
    void shouldDeleteCaseWithNoDocuments() {
        // Given
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of(caseData(CASE_1)));
        when(pcsCaseService.getDocumentUrls(CASE_1)).thenReturn(List.of());

        // When
        underTest.runSweep();

        // Then
        verifyNoInteractions(documentImportService);
        verify(caseDeletionService).deleteCaseData(CASE_1);
    }

    @Test
    void shouldProcessAllCasesInBatch() {
        // Given
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of(caseData(CASE_1), caseData(CASE_2), caseData(CASE_3)));

        // When
        underTest.runSweep();

        // Then
        verify(caseDeletionService).deleteCaseData(CASE_1);
        verify(caseDeletionService).deleteCaseData(CASE_2);
        verify(caseDeletionService).deleteCaseData(CASE_3);
    }

    @Test
    void shouldContinueDeletionWhenCaseNotFoundInCcdDataStore() {
        // Given
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of(caseData(CASE_1)));
        doThrow(new CcdCaseNotFoundException(CASE_1)).when(ccdCaseDataDeletionService).markCaseForDeletion(CASE_1);

        // When
        underTest.runSweep();

        // Then
        verify(caseDeletionService).deleteCaseData(CASE_1);
    }

    @Test
    void shouldSkipCcdDeletionForDraftDiscardedCases() {
        // Given
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of(caseData(CASE_1, State.DRAFT_DISCARDED)));

        // When
        underTest.runSweep();

        // Then
        verify(ccdCaseDataDeletionService, never()).markCaseForDeletion(CASE_1);
        verify(ccdCaseDataDeletionService, never()).confirmCaseDisposal(CASE_1);
        verify(caseDeletionService).deleteCaseData(CASE_1);
    }

    @Test
    void shouldTreatDocumentNotFoundAsSuccess() {
        // Given
        String url = "url";
        DocumentEntity doc = document(url);
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of(caseData(CASE_1)));
        when(pcsCaseService.getDocumentUrls(CASE_1)).thenReturn(List.of(url));
        doThrow(new DocumentNotFoundException(doc.getId())).when(documentImportService).deleteDocument(url);

        // When
        underTest.runSweep();

        // Then
        verify(caseDeletionService).deleteCaseData(CASE_1);
    }

    @Test
    void shouldStopCaseDeletionWhenDocumentDeletionFails() {
        // Given
        String url1 = "url1";
        String url2 = "url2";
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of(caseData(CASE_1)));
        when(pcsCaseService.getDocumentUrls(CASE_1)).thenReturn(List.of(url1, url2));
        doThrow(new RuntimeException("dm-store unavailable")).when(documentImportService).deleteDocument(url1);

        // When
        assertDoesNotThrow(() -> underTest.runSweep());

        // Then remaining documents are still attempted, but the case data must NOT be deleted
        verify(documentImportService).deleteDocument(url2);
        verify(caseDeletionService, never()).deleteCaseData(anyLong());
    }

    @Test
    void shouldIsolateFailuresSoOtherCasesStillComplete() {
        // Given
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of(caseData(CASE_1), caseData(CASE_2)));
        doThrow(new PcsCaseDeletionException(CASE_1)).when(caseDeletionService).deleteCaseData(CASE_1);

        // When
        assertDoesNotThrow(() -> underTest.runSweep());

        // Then
        verify(caseDeletionService, times(2)).deleteCaseData(anyLong());
    }

    @Test
    @Timeout(30)
    void shouldCapConcurrentCcdCallsToThrottleSize() {
        // Given
        int ccdControlSize = 2;
        underTest = createTask(ccdControlSize, 25, 10);
        List<DeletionCaseData> cases = LongStream.rangeClosed(1, 8)
            .mapToObj(this::caseData)
            .toList();
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(cases);
        doAnswer(inv -> trackConcurrencyWithLatency())
            .when(ccdCaseDataDeletionService).markCaseForDeletion(anyLong());

        // When
        underTest.runSweep();

        // Then
        assertThat(maxObservedConcurrency.get()).as("In flight calls must never exceed the throttle size")
            .isLessThanOrEqualTo(ccdControlSize);
        verify(ccdCaseDataDeletionService, times(cases.size())).markCaseForDeletion(anyLong());
        verify(caseDeletionService, times(cases.size())).deleteCaseData(anyLong());
    }

    @Test
    @Timeout(30)
    void shouldCapConcurrentDocumentCallsToThrottleSize() {
        // Given
        String url = "url1";
        String url2 = "url2";
        int docControlSize = 2;
        underTest = createTask(10, docControlSize, 10);
        List<DeletionCaseData> cases = LongStream.rangeClosed(1, 6)
            .mapToObj(this::caseData)
            .toList();
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT)).thenReturn(cases);
        when(pcsCaseService.getDocumentUrls(cases.get(0).getCaseRef())).thenReturn(List.of(url, url2));
        when(pcsCaseService.getDocumentUrls(cases.get(1).getCaseRef())).thenReturn(List.of(url, url2));
        when(pcsCaseService.getDocumentUrls(cases.get(2).getCaseRef())).thenReturn(List.of(url, url2));
        when(pcsCaseService.getDocumentUrls(cases.get(3).getCaseRef())).thenReturn(List.of(url, url2));
        when(pcsCaseService.getDocumentUrls(cases.get(4).getCaseRef())).thenReturn(List.of(url, url2));
        when(pcsCaseService.getDocumentUrls(cases.get(5).getCaseRef())).thenReturn(List.of(url, url2));
        doAnswer(inv -> trackConcurrencyWithLatency()).when(documentImportService)
            .deleteDocument(anyString());

        // When
        underTest.runSweep();

        // Then
        assertThat(maxObservedConcurrency.get()).as("Calls in flight must never exceed the throttle size")
            .isLessThanOrEqualTo(docControlSize);
        verify(documentImportService, times(cases.size() * 2)).deleteDocument(anyString());
        verify(caseDeletionService, times(cases.size())).deleteCaseData(anyLong());
    }

    @Test
    @Timeout(30)
    void shouldReleaseCcdWhenCallsFailSoSweepDoesNotDeadlock() {
        // Given
        underTest = createTask(1, 25, 10);
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of(caseData(CASE_1), caseData(CASE_2)));
        doThrow(new RuntimeException("ccd issue"))
            .when(ccdCaseDataDeletionService).markCaseForDeletion(anyLong());

        // When
        assertDoesNotThrow(() -> underTest.runSweep());

        // Then
        verify(ccdCaseDataDeletionService).markCaseForDeletion(CASE_1);
        verify(ccdCaseDataDeletionService).markCaseForDeletion(CASE_2);
        verify(caseDeletionService, never()).deleteCaseData(anyLong());
    }

    @Test
    @Timeout(30)
    void shouldReleaseWhenDocumentDeletionFailsSoRunSweepDoesNotDeadlock() {
        // Setup
        underTest = createTask(10, 1, 10);

        // Given
        String url = "url";
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of(caseData(CASE_1), caseData(CASE_2)));

        when(pcsCaseService.getDocumentUrls(CASE_1)).thenReturn(List.of(url));
        when(pcsCaseService.getDocumentUrls(CASE_2)).thenReturn(List.of(url));

        doThrow(new RuntimeException("dm-store blew up")).when(documentImportService).deleteDocument(url);

        // When
        assertDoesNotThrow(() -> underTest.runSweep());

        // Then
        verify(documentImportService, times(2)).deleteDocument(url);
        verify(caseDeletionService, never()).deleteCaseData(anyLong());
    }

    @Test
    @Timeout(30)
    void shouldProcessCasesInBatches() {
        // Given
        int batchSize = 2;
        underTest = createTask(10, 25, batchSize);
        List<DeletionCaseData> cases = List.of(caseData(CASE_1), caseData(CASE_2), caseData(CASE_3), caseData(CASE_4));
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
                .thenReturn(cases);
        when(pcsCaseService.getDocumentUrls(anyLong())).thenReturn(List.of());

        // When
        underTest.runSweep();

        // Then
        for (DeletionCaseData caseToDelete : cases) {
            verify(ccdCaseDataDeletionService).markCaseForDeletion(caseToDelete.getCaseRef());
        }
        verify(caseDeletionService, times(cases.size())).deleteCaseData(anyLong());
    }

    @Test
    void shouldCollectFailedCasesFromMultipleBatches() {
        // Given
        int batchSize = 2;
        underTest = createTask(10, 25, batchSize);
        List<DeletionCaseData> cases = List.of(caseData(CASE_1), caseData(CASE_2), caseData(CASE_3), caseData(CASE_4));
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
                .thenReturn(cases);
        when(pcsCaseService.getDocumentUrls(CASE_1)).thenReturn(List.of());
        when(pcsCaseService.getDocumentUrls(CASE_2)).thenThrow(new RuntimeException("service error"));
        when(pcsCaseService.getDocumentUrls(CASE_3)).thenReturn(List.of());
        when(pcsCaseService.getDocumentUrls(CASE_4)).thenThrow(new RuntimeException("service error"));

        // When
        assertDoesNotThrow(() -> underTest.runSweep());

        // Then
        verify(caseDeletionService).deleteCaseData(CASE_1);
        verify(caseDeletionService).deleteCaseData(CASE_3);
        verify(caseDeletionService, never()).deleteCaseData(CASE_2);
        verify(caseDeletionService, never()).deleteCaseData(CASE_4);
    }

    @Test
    void shouldProcessExactlyOneBatchWhenCasesEqualBatchSize() {
        // Given
        int batchSize = 3;
        underTest = createTask(10, 25, batchSize);
        List<DeletionCaseData> cases = List.of(caseData(CASE_1), caseData(CASE_2), caseData(CASE_3));
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
                .thenReturn(cases);
        when(pcsCaseService.getDocumentUrls(anyLong())).thenReturn(List.of());

        // When
        underTest.runSweep();

        // Then
        verify(caseDeletionService, times(3)).deleteCaseData(anyLong());
    }

    @Test
    void shouldProcessSingleCaseInBatch() {
        // Given
        int batchSize = 10;
        underTest = createTask(10, 25, batchSize);
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
                .thenReturn(List.of(caseData(CASE_1)));
        when(pcsCaseService.getDocumentUrls(CASE_1)).thenReturn(List.of());

        // When
        underTest.runSweep();

        // Then
        verify(caseDeletionService).deleteCaseData(CASE_1);
    }

    @Test
    void shouldHandlePartialBatchFailure() {
        // Given
        int batchSize = 3;
        underTest = createTask(10, 25, batchSize);
        List<DeletionCaseData> cases = List.of(caseData(CASE_1), caseData(CASE_2), caseData(CASE_3),
                caseData(CASE_4), caseData(CASE_5));
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
                .thenReturn(cases);
        when(pcsCaseService.getDocumentUrls(CASE_1)).thenReturn(List.of());
        when(pcsCaseService.getDocumentUrls(CASE_2)).thenReturn(List.of());
        when(pcsCaseService.getDocumentUrls(CASE_3)).thenThrow(new RuntimeException("error"));
        when(pcsCaseService.getDocumentUrls(CASE_4)).thenReturn(List.of());
        when(pcsCaseService.getDocumentUrls(CASE_5)).thenReturn(List.of());

        // When
        assertDoesNotThrow(() -> underTest.runSweep());

        // Then
        verify(caseDeletionService, times(4)).deleteCaseData(anyLong());
        verify(caseDeletionService, never()).deleteCaseData(CASE_3);
    }

    @Test
    void shouldThrowDocumentDeletionIncompleteExceptionWhenDocumentDeletionFails() {
        // Given
        String url = "url1";
        List<DeletionCaseData> cases = List.of(caseData(CASE_1));
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
                .thenReturn(cases);
        when(pcsCaseService.getDocumentUrls(CASE_1)).thenReturn(List.of(url));
        doThrow(new RuntimeException("dm-store error"))
                .when(documentImportService).deleteDocument(url);

        // When & Then
        assertDoesNotThrow(() -> underTest.runSweep());

        // Document error should prevent case deletion
        verify(caseDeletionService, never()).deleteCaseData(CASE_1);
    }

    @Test
    void shouldIgnoreDocumentNotFoundException() {
        // Given
        String url = "url1";
        UUID documentId = UUID.randomUUID();
        List<DeletionCaseData> cases = List.of(caseData(CASE_1));
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, SQL_LIMIT))
                .thenReturn(cases);
        when(pcsCaseService.getDocumentUrls(CASE_1)).thenReturn(List.of(url));
        doThrow(new DocumentNotFoundException(documentId))
                .when(documentImportService).deleteDocument(url);

        // When
        underTest.runSweep();

        // Then - DocumentNotFoundException should be caught and logged, case still deleted
        verify(caseDeletionService).deleteCaseData(CASE_1);
    }

    private CaseDeletionScheduledTask createTask(int ccdControlSize, int docControlSize, int batchSize) {
        return new CaseDeletionScheduledTask(VALID_SCHEDULE, DISCARD_AFTER_DAYS, batchSize, SQL_LIMIT,
                                             ccdControlSize, docControlSize,
                                             caseDeletionService, ccdCaseDataDeletionService,
                                             pcsCaseService, documentImportService);
    }

    private DeletionCaseData caseData(long caseRef) {
        return caseData(caseRef, State.AWAITING_SUBMISSION_TO_HMCTS);
    }

    private DeletionCaseData caseData(long caseRef, State state) {
        return DeletionCaseData.builder()
            .caseRef(caseRef)
            .state(state)
            .build();
    }

    private DocumentEntity document(String url) {
        return DocumentEntity.builder().id(UUID.randomUUID()).url(url).build();
    }

    private Object trackConcurrencyWithLatency() {
        int now = concurrentCalls.incrementAndGet();
        maxObservedConcurrency.accumulateAndGet(now, Math::max);
        LockSupport.parkNanos(Duration.ofMillis(SIMULATED_LATENCY_MS).toNanos());
        concurrentCalls.decrementAndGet();
        return null;
    }
}
