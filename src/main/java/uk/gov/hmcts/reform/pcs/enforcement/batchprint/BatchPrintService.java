package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterDocumentFetcher;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.PdfMerger;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces a bailiff's batch print (HDPI-8012): one PDF holding, for each case in turn, its warrant, risk
 * assessment and notice of eviction.
 *
 * <p>The bailiff downloads the file and prints it themselves. Nothing is posted, so Send Letter is not used,
 * and nothing is stored: the file is built on request from the documents already in each case's Enforcement
 * folder. Documents are fetched from CDAM as the system user, the same way bulk print fetches them; the
 * bailiff's right to each case is checked by {@link BatchPrintPlanner} first.
 *
 * <p>Each case is fetched and merged on its own, so a document that cannot be fetched or read leaves only its
 * case out, and the bailiff is told which.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchPrintService {

    private final BatchPrintPlanner batchPrintPlanner;
    private final LetterDocumentFetcher letterDocumentFetcher;
    private final PdfMerger pdfMerger;

    public BatchPrint print(String bailiffUserId, List<String> caseReferences) {
        BatchPrintPlan plan = batchPrintPlanner.plan(bailiffUserId, caseReferences);

        List<byte[]> casePdfs = new ArrayList<>();
        List<ExcludedCase> unavailable = new ArrayList<>();
        for (PrintableCase printable : plan.included()) {
            try {
                casePdfs.add(mergeCase(printable));
            } catch (RuntimeException e) {
                log.warn("Batch print left out case {}: its documents could not be fetched or merged",
                         printable.caseReference(), e);
                unavailable.add(ExcludedCase.of(printable.caseReference(), BatchPrintExclusion.DOCUMENT_UNAVAILABLE));
            }
        }

        BatchPrintPlan printed = plan.exclude(unavailable);
        log.info("Batch print for bailiff {}: {} case(s) printed, {} left out", bailiffUserId,
                 printed.included().size(), printed.excluded().size());
        return new BatchPrint(casePdfs.isEmpty() ? null : pdfMerger.merge(casePdfs), printed);
    }

    private byte[] mergeCase(PrintableCase printable) {
        List<byte[]> documents = printable.documents().stream()
            .map(document -> letterDocumentFetcher.fetchBytes(document.documentId()))
            .toList();
        return pdfMerger.merge(documents);
    }
}
