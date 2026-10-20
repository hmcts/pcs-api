package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterDocumentFetcher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * At most {@value #MAX_CONCURRENT_CASES} cases are fetched at a time across every batch print being built.
 */
@Slf4j
@Service
public class BatchPrintService {

    static final int MAX_CONCURRENT_CASES = 6;

    static final Duration MAX_WAIT_FOR_A_SLOT = Duration.ofSeconds(30);

    private final BatchPrintPlanner batchPrintPlanner;
    private final LetterDocumentFetcher letterDocumentFetcher;
    private final BatchPrintPdfMerger pdfMerger;
    private final Semaphore caseSlots;
    private final int maxConcurrentCases;
    private final Duration maxWaitForASlot;

    @Autowired
    public BatchPrintService(BatchPrintPlanner batchPrintPlanner,
                             LetterDocumentFetcher letterDocumentFetcher,
                             BatchPrintPdfMerger pdfMerger) {
        this(batchPrintPlanner, letterDocumentFetcher, pdfMerger, MAX_CONCURRENT_CASES, MAX_WAIT_FOR_A_SLOT);
    }

    BatchPrintService(BatchPrintPlanner batchPrintPlanner,
                      LetterDocumentFetcher letterDocumentFetcher,
                      BatchPrintPdfMerger pdfMerger,
                      int maxConcurrentCases,
                      Duration maxWaitForASlot) {
        this.batchPrintPlanner = batchPrintPlanner;
        this.letterDocumentFetcher = letterDocumentFetcher;
        this.pdfMerger = pdfMerger;
        this.caseSlots = new Semaphore(maxConcurrentCases, true);
        this.maxConcurrentCases = maxConcurrentCases;
        this.maxWaitForASlot = maxWaitForASlot;
    }

    /**
     * The temp directory is the caller's to delete; a refused or empty print leaves none behind.
     */
    public BatchPrint print(String bailiffUserId, List<String> caseReferences) {
        Path directory = createMergeDirectory();
        boolean handedOver = false;
        try {
            BatchPrint batchPrint = buildBatchPrint(bailiffUserId, caseReferences, directory);
            handedOver = !batchPrint.isEmpty();
            return batchPrint;
        } finally {
            if (!handedOver) {
                BatchPrintTempDirectory.delete(directory);
            }
        }
    }

    private BatchPrint buildBatchPrint(String bailiffUserId, List<String> caseReferences, Path directory) {
        BatchPrintPlan plan = batchPrintPlanner.plan(bailiffUserId, caseReferences);

        Duration slotBudget = slotBudget(plan.included().size());
        List<CaseFile> caseFiles = mergeEachCase(directory, plan.included(), Instant.now().plus(slotBudget));
        try {
            if (caseFiles.stream().anyMatch(CaseFile::tooBusy)) {
                log.warn("Batch print for bailiff {} refused: no slot for {} of its {} case(s) within {}",
                         bailiffUserId, caseFiles.stream().filter(CaseFile::tooBusy).count(), caseFiles.size(),
                         slotBudget);
                throw new BatchPrintBusyException();
            }
            List<ExcludedCase> unavailable = caseFiles.stream()
                .filter(caseFile -> caseFile.file() == null)
                .map(caseFile -> ExcludedCase.of(caseFile.caseReference(), BatchPrintExclusion.DOCUMENT_UNAVAILABLE))
                .toList();
            List<Path> printedFiles = caseFiles.stream().map(CaseFile::file).filter(Objects::nonNull).toList();

            BatchPrintPlan printed = plan.exclude(unavailable);
            log.info("Batch print for bailiff {}: {} case(s) printed, {} left out", bailiffUserId,
                     printed.included().size(), printed.excluded().size());
            if (printedFiles.isEmpty()) {
                return new BatchPrint(null, printed, null);
            }
            return new BatchPrint(mergeCases(directory, printedFiles), printed, directory);
        } finally {
            caseFiles.forEach(CaseFile::delete);
        }
    }

    private Path createMergeDirectory() {
        try {
            return pdfMerger.createMergeDirectory();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create the batch print temp directory", e);
        }
    }

    private Duration slotBudget(int caseCount) {
        return maxWaitForASlot.multipliedBy(Math.max(1, Math.ceilDiv(caseCount, maxConcurrentCases)));
    }

    private List<CaseFile> mergeEachCase(Path directory, List<PrintableCase> printableCases, Instant waitUntil) {
        List<Future<CaseFile>> futures = new ArrayList<>();
        // Closing the executor waits for every case, so none is still being written once this returns.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            printableCases.forEach(
                printable -> futures.add(executor.submit(() -> mergeCase(directory, printable, waitUntil))));
        }
        List<CaseFile> caseFiles = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            Future<CaseFile> future = futures.get(i);
            caseFiles.add(future.state() == Future.State.SUCCESS
                              ? future.resultNow()
                              : CaseFile.unavailable(printableCases.get(i).caseReference()));
        }
        return caseFiles;
    }

    private CaseFile mergeCase(Path directory, PrintableCase printable, Instant waitUntil) {
        try {
            if (!caseSlots.tryAcquire(millisUntil(waitUntil), TimeUnit.MILLISECONDS)) {
                return CaseFile.busy(printable.caseReference());
            }
            try {
                List<byte[]> documents = printable.documents().stream()
                    .map(document -> letterDocumentFetcher.fetchBytes(document.documentId()))
                    .toList();
                return CaseFile.merged(printable.caseReference(), pdfMerger.mergeCase(directory, documents));
            } finally {
                caseSlots.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CaseFile.unavailable(printable.caseReference());
        } catch (Exception e) {
            log.warn("Batch print left out case {}: its documents could not be fetched or merged",
                     printable.caseReference(), e);
            return CaseFile.unavailable(printable.caseReference());
        }
    }

    private static long millisUntil(Instant waitUntil) {
        return Math.max(0, Duration.between(Instant.now(), waitUntil).toMillis());
    }

    private Path mergeCases(Path directory, List<Path> caseFiles) {
        try {
            return pdfMerger.mergeCases(directory, caseFiles);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to merge the batch print", e);
        }
    }

    private record CaseFile(String caseReference, Path file, boolean tooBusy) {

        static CaseFile merged(String caseReference, Path file) {
            return new CaseFile(caseReference, file, false);
        }

        static CaseFile unavailable(String caseReference) {
            return new CaseFile(caseReference, null, false);
        }

        static CaseFile busy(String caseReference) {
            return new CaseFile(caseReference, null, true);
        }

        void delete() {
            if (file == null) {
                return;
            }
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("Could not delete the batch print temp file {}", file, e);
            }
        }
    }
}
