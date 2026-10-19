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
 * Produces a bailiff's batch print (HDPI-8012): one PDF holding, for each case in turn, its warrant, risk
 * assessment and notice of eviction.
 *
 * <p>The bailiff downloads the file and prints it themselves. Nothing is posted, so Send Letter is not used,
 * and nothing is stored: the file is built on request from the documents already in each case's Enforcement
 * folder. Documents are fetched from CDAM as the system user, the same way bulk print fetches them; the
 * bailiff's right to each case is checked by {@link BatchPrintPlanner} first.
 *
 * <p>Cases are fetched and merged in parallel, at most {@value #MAX_CONCURRENT_CASES} at a time across every
 * batch print the instance is building, and each case is merged into a file of its own in the request's
 * {@link BatchPrintTempDirectory} ({@link BatchPrintPdfMerger}). That bounds both the load on CDAM and the
 * documents held in memory, whatever the number of cases or of bailiffs printing at once. A document that
 * cannot be fetched or read leaves only its case out, and the bailiff is told which; a request whose cases
 * cannot get their turn at those slots within {@link #MAX_WAIT_FOR_A_SLOT} per wave of
 * {@value #MAX_CONCURRENT_CASES} is refused whole with {@link BatchPrintBusyException}.
 */
@Slf4j
@Service
public class BatchPrintService {

    static final int MAX_CONCURRENT_CASES = 6;

    /**
     * How long a request will wait for its turn at the case slots, for each wave of
     * {@value #MAX_CONCURRENT_CASES} cases it has to get through. {@link #print} runs on the request thread, so
     * an unbounded wait holds that thread for as long as every other batch print being built takes, which
     * outlives the gateway's own timeout and leaves the bailiff nothing to read.
     */
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
     * Builds the batch print, in a temp directory the caller owns and has to delete. A request that is refused
     * or that has nothing to print leaves no directory behind.
     *
     * @throws BatchPrintBusyException if the instance is too busy to fetch this request's documents
     */
    public BatchPrint print(String bailiffUserId, List<String> caseReferences) {
        Path directory = createMergeDirectory();
        // The caller only takes the directory on with the file it has to stream, so a refusal and a print with
        // nothing in it both leave it here to delete.
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

    /**
     * How long the request as a whole will wait at the case slots: {@link #MAX_WAIT_FOR_A_SLOT} for each wave of
     * {@value #MAX_CONCURRENT_CASES} cases, because only that many of its own cases can be fetched at a time.
     * A single budget for the whole request would be spent queueing behind its own cases, so the largest
     * batches would refuse themselves however idle the instance was; a budget per wave is spent only on the
     * batch prints other bailiffs are having built.
     */
    private Duration slotBudget(int caseCount) {
        return maxWaitForASlot.multipliedBy(Math.max(1, Math.ceilDiv(caseCount, maxConcurrentCases)));
    }

    /**
     * Each case's merged file, in the order given, with no file for a case that could not be fetched or merged.
     */
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

    /**
     * One case's merged documents; no file when they could not be fetched or merged, and too busy when the
     * case never got a slot to fetch them in.
     */
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
