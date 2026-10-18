package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterDocumentFetcher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

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
 * batch print the instance is building, and each case is merged into a temp file of its own
 * ({@link BatchPrintPdfMerger}). That bounds both the load on CDAM and the documents held in memory, whatever
 * the number of cases or of bailiffs printing at once. A document that cannot be fetched or read leaves only its
 * case out, and the bailiff is told which.
 */
@Slf4j
@Service
public class BatchPrintService {

    static final int MAX_CONCURRENT_CASES = 6;

    private final BatchPrintPlanner batchPrintPlanner;
    private final LetterDocumentFetcher letterDocumentFetcher;
    private final BatchPrintPdfMerger pdfMerger;
    private final Semaphore caseSlots;

    @Autowired
    public BatchPrintService(BatchPrintPlanner batchPrintPlanner,
                             LetterDocumentFetcher letterDocumentFetcher,
                             BatchPrintPdfMerger pdfMerger) {
        this(batchPrintPlanner, letterDocumentFetcher, pdfMerger, MAX_CONCURRENT_CASES);
    }

    BatchPrintService(BatchPrintPlanner batchPrintPlanner,
                      LetterDocumentFetcher letterDocumentFetcher,
                      BatchPrintPdfMerger pdfMerger,
                      int maxConcurrentCases) {
        this.batchPrintPlanner = batchPrintPlanner;
        this.letterDocumentFetcher = letterDocumentFetcher;
        this.pdfMerger = pdfMerger;
        this.caseSlots = new Semaphore(maxConcurrentCases, true);
    }

    public BatchPrint print(String bailiffUserId, List<String> caseReferences) {
        BatchPrintPlan plan = batchPrintPlanner.plan(bailiffUserId, caseReferences);

        List<CaseFile> caseFiles = mergeEachCase(plan.included());
        try {
            List<ExcludedCase> unavailable = caseFiles.stream()
                .filter(caseFile -> caseFile.file() == null)
                .map(caseFile -> ExcludedCase.of(caseFile.caseReference(), BatchPrintExclusion.DOCUMENT_UNAVAILABLE))
                .toList();
            List<Path> printedFiles = caseFiles.stream().map(CaseFile::file).filter(Objects::nonNull).toList();

            BatchPrintPlan printed = plan.exclude(unavailable);
            log.info("Batch print for bailiff {}: {} case(s) printed, {} left out", bailiffUserId,
                     printed.included().size(), printed.excluded().size());
            return new BatchPrint(printedFiles.isEmpty() ? null : mergeCases(printedFiles), printed);
        } finally {
            caseFiles.forEach(CaseFile::delete);
        }
    }

    /**
     * Each case's merged file, in the order given, with no file for a case that could not be fetched or merged.
     */
    private List<CaseFile> mergeEachCase(List<PrintableCase> printableCases) {
        List<Future<CaseFile>> futures = new ArrayList<>();
        // Closing the executor waits for every case, so none is still being written once this returns.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            printableCases.forEach(printable -> futures.add(executor.submit(() -> mergeCase(printable))));
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

    private CaseFile mergeCase(PrintableCase printable) {
        try {
            caseSlots.acquire();
            try {
                List<byte[]> documents = printable.documents().stream()
                    .map(document -> letterDocumentFetcher.fetchBytes(document.documentId()))
                    .toList();
                return new CaseFile(printable.caseReference(), pdfMerger.mergeCase(documents));
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

    private BatchPrintFile mergeCases(List<Path> caseFiles) {
        try {
            return new BatchPrintFile(pdfMerger.mergeCases(caseFiles));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to merge the batch print", e);
        }
    }

    /**
     * One case's merged documents, or no file when they could not be fetched or merged.
     */
    private record CaseFile(String caseReference, Path file) {

        static CaseFile unavailable(String caseReference) {
            return new CaseFile(caseReference, null);
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
