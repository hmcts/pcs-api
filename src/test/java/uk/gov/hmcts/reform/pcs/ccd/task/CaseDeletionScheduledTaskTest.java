package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.DocumentNotFoundException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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

    private static final String VALID_SCHEDULE = "DAILY|00:00";
    private static final int DISCARD_AFTER_DAYS = 30;
    private static final int BATCH_SIZE = 50;
    private static final long SIMULATED_LATENCY_MS = 100;
    private static final long CASE_1 = 1L;
    private static final long CASE_2 = 2L;
    private static final long CASE_3 = 3L;

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
        underTest = createTask(10, 25);
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
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
            .thenReturn(List.of());

        // When
        underTest.runSweep();

        // Then
        verifyNoInteractions(pcsCaseService, caseDeletionService, documentImportService);
    }

    @Test
    void shouldDoNothingWhenExpiredCasesIsNull() {
        // Given
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
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
        DocumentEntity doc1 = document(url1);
        DocumentEntity doc2 = document(url2);
        PcsCaseEntity entity = newPcsCaseEntity(List.of(doc1, doc2));
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
            .thenReturn(List.of(CASE_1));
        when(pcsCaseService.loadCase(CASE_1)).thenReturn(entity);

        // When
        underTest.runSweep();

        // Then
        InOrder inOrder = inOrder(ccdCaseDataDeletionService, documentImportService, caseDeletionService);
        inOrder.verify(ccdCaseDataDeletionService).markCaseForDeletion(CASE_1);
        inOrder.verify(ccdCaseDataDeletionService).confirmCaseDisposal(CASE_1);
        inOrder.verify(documentImportService).deleteDocument(url1);
        inOrder.verify(documentImportService).deleteDocument(url2);
        inOrder.verify(caseDeletionService).deleteCaseData(entity);
    }

    @Test
    void shouldDeleteCaseWithNoDocuments() {
        // Given
        PcsCaseEntity entity = newPcsCaseEntity(List.of());
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
            .thenReturn(List.of(CASE_1));
        when(pcsCaseService.loadCase(CASE_1)).thenReturn(entity);

        // When
        underTest.runSweep();

        // Then
        verifyNoInteractions(documentImportService);
        verify(caseDeletionService).deleteCaseData(entity);
    }

    @Test
    void shouldProcessAllCasesInBatch() {
        // Given
        PcsCaseEntity case1 = newPcsCaseEntity(List.of());
        PcsCaseEntity case2 = newPcsCaseEntity(List.of());
        PcsCaseEntity case3 = newPcsCaseEntity(List.of());
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
            .thenReturn(List.of(CASE_1, CASE_2, CASE_3));
        when(pcsCaseService.loadCase(CASE_1)).thenReturn(case1);
        when(pcsCaseService.loadCase(CASE_2)).thenReturn(case2);
        when(pcsCaseService.loadCase(CASE_3)).thenReturn(case3);

        // When
        underTest.runSweep();

        // Then
        verify(caseDeletionService).deleteCaseData(case1);
        verify(caseDeletionService).deleteCaseData(case2);
        verify(caseDeletionService).deleteCaseData(case3);
    }

    @Test
    void shouldContinueDeletionWhenCaseNotFoundInCcdDataStore() {
        // Given
        PcsCaseEntity entity = newPcsCaseEntity(List.of());
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
            .thenReturn(List.of(CASE_1));
        doThrow(new CcdCaseNotFoundException(CASE_1)).when(ccdCaseDataDeletionService).markCaseForDeletion(CASE_1);
        when(pcsCaseService.loadCase(CASE_1)).thenReturn(entity);

        // When
        underTest.runSweep();

        // Then
        verify(caseDeletionService).deleteCaseData(entity);
    }

    @Test
    void shouldTreatDocumentNotFoundAsSuccess() {
        // Given
        String url = "url";
        DocumentEntity doc = document(url);
        PcsCaseEntity entity = newPcsCaseEntity(List.of(doc));
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
            .thenReturn(List.of(CASE_1));
        when(pcsCaseService.loadCase(CASE_1)).thenReturn(entity);
        doThrow(new DocumentNotFoundException(doc.getId())).when(documentImportService).deleteDocument(url);

        // When
        underTest.runSweep();

        // Then
        verify(caseDeletionService).deleteCaseData(entity);
    }

    @Test
    void shouldStopCaseDeletionWhenDocumentDeletionFails() {
        // Given
        String url1 = "url1";
        String url2 = "url2";
        DocumentEntity failingDoc = document(url1);
        DocumentEntity okDoc = document(url2);
        PcsCaseEntity entity = newPcsCaseEntity(List.of(failingDoc, okDoc));
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
            .thenReturn(List.of(CASE_1));
        when(pcsCaseService.loadCase(CASE_1)).thenReturn(entity);
        doThrow(new RuntimeException("dm-store unavailable")).when(documentImportService).deleteDocument(url1);

        // When
        assertDoesNotThrow(() -> underTest.runSweep());

        // Then remaining documents are still attempted, but the case data must NOT be deleted
        verify(documentImportService).deleteDocument(url2);
        verify(caseDeletionService, never()).deleteCaseData(any());
    }

    @Test
    void shouldIsolateFailuresSoOtherCasesStillComplete() {
        // Given
        PcsCaseEntity pcsCaseEntity = newPcsCaseEntity(List.of());
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
            .thenReturn(List.of(CASE_1, CASE_2));
        when(pcsCaseService.loadCase(CASE_1)).thenThrow(new RuntimeException("db error"));
        when(pcsCaseService.loadCase(CASE_2)).thenReturn(pcsCaseEntity);

        // When
        assertDoesNotThrow(() -> underTest.runSweep());

        // Then
        verify(caseDeletionService, times(1)).deleteCaseData(any());
        verify(caseDeletionService).deleteCaseData(pcsCaseEntity);
    }

    @Test
    @Timeout(30)
    void shouldCapConcurrentCcdCallsToThrottleSize() {
        // Given
        int ccdControlSize = 2;
        underTest = createTask(ccdControlSize, 25);
        List<Long> cases = LongStream.rangeClosed(1, 8).boxed().toList();
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
            .thenReturn(cases);
        doAnswer(inv -> trackConcurrencyWithLatency())
            .when(ccdCaseDataDeletionService).markCaseForDeletion(anyLong());
        when(pcsCaseService.loadCase(anyLong()))
            .thenAnswer(inv -> newPcsCaseEntity(List.of()));

        // When
        underTest.runSweep();

        // Then
        assertThat(maxObservedConcurrency.get()).as("In flight calls must never exceed the throttle size")
            .isLessThanOrEqualTo(ccdControlSize);
        verify(ccdCaseDataDeletionService, times(cases.size())).markCaseForDeletion(anyLong());
        verify(caseDeletionService, times(cases.size())).deleteCaseData(any());
    }

    @Test
    @Timeout(30)
    void shouldCapConcurrentDocumentCallsToThrottleSize() {
        // Given
        String url = "url1";
        String url2 = "url2";
        int docControlSize = 2;
        underTest = createTask(10, docControlSize);
        List<Long> cases = LongStream.rangeClosed(1, 6).boxed().toList();
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE)).thenReturn(cases);
        when(pcsCaseService.loadCase(anyLong()))
            .thenAnswer(inv -> newPcsCaseEntity(List.of(document(url), document(url2))));
        doAnswer(inv -> trackConcurrencyWithLatency()).when(documentImportService)
            .deleteDocument(anyString());

        // When
        underTest.runSweep();

        // Then
        assertThat(maxObservedConcurrency.get()).as("Calls in flight must never exceed the throttle size")
            .isLessThanOrEqualTo(docControlSize);
        verify(documentImportService, times(cases.size() * 2)).deleteDocument(anyString());
        verify(caseDeletionService, times(cases.size())).deleteCaseData(any());
    }

    @Test
    @Timeout(30)
    void shouldReleaseCcdWhenCallsFailSoSweepDoesNotDeadlock() {
        // Given
        underTest = createTask(1, 25);
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
            .thenReturn(List.of(CASE_1, CASE_2));
        doThrow(new RuntimeException("ccd issue"))
            .when(ccdCaseDataDeletionService).markCaseForDeletion(anyLong());

        // When
        assertDoesNotThrow(() -> underTest.runSweep());

        // Then
        verify(ccdCaseDataDeletionService).markCaseForDeletion(CASE_1);
        verify(ccdCaseDataDeletionService).markCaseForDeletion(CASE_2);
        verify(caseDeletionService, never()).deleteCaseData(any());
    }

    @Test
    @Timeout(30)
    void shouldReleaseWhenDocumentDeletionFailsSoRunSweepDoesNotDeadlock() {
        // Setup
        underTest = createTask(10, 1);

        // Given
        String url = "url";
        when(ccdCaseDataDeletionService.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, BATCH_SIZE))
            .thenReturn(List.of(CASE_1, CASE_2));
        when(pcsCaseService.loadCase(anyLong()))
            .thenAnswer(inv -> newPcsCaseEntity(List.of(document(url))));
        doThrow(new RuntimeException("dm-store blew up")).when(documentImportService).deleteDocument(url);

        // When
        assertDoesNotThrow(() -> underTest.runSweep());

        // Then
        verify(documentImportService, times(2)).deleteDocument(url);
        verify(caseDeletionService, never()).deleteCaseData(any());
    }

    private CaseDeletionScheduledTask createTask(int ccdControlSize, int docControlSize) {
        return new CaseDeletionScheduledTask(VALID_SCHEDULE, DISCARD_AFTER_DAYS, BATCH_SIZE,
                                             ccdControlSize, docControlSize,
                                             caseDeletionService, ccdCaseDataDeletionService,
                                             pcsCaseService, documentImportService);
    }

    private PcsCaseEntity newPcsCaseEntity(List<DocumentEntity> documents) {
        PcsCaseEntity entity = mock(PcsCaseEntity.class);
        when(entity.getDocuments()).thenReturn(documents);
        return entity;
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
