package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Merges batch print PDFs into temp files rather than into memory. Each case is merged on its own, then the
 * cases' files are merged into the one file the bailiff downloads, with PDFBox caching the merged content in
 * temp files too. Only the documents of the cases being fetched at the time are held in memory, however many
 * cases are printed.
 */
@Component
public class BatchPrintPdfMerger {

    private static final String TEMP_FILE_PREFIX = "batch-print-";
    private static final String TEMP_FILE_SUFFIX = ".pdf";

    /**
     * Merges one case's documents, in order, into a new temp file the caller must delete.
     *
     * @throws IOException if a document is not a readable PDF
     */
    public Path mergeCase(List<byte[]> documents) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        documents.forEach(document -> merger.addSource(new RandomAccessReadBuffer(document)));
        return mergeToTempFile(merger);
    }

    /**
     * Merges the cases' files, in order, into a new temp file the caller must delete.
     */
    public Path mergeCases(List<Path> caseFiles) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        for (Path caseFile : caseFiles) {
            merger.addSource(caseFile.toFile());
        }
        return mergeToTempFile(merger);
    }

    private static Path mergeToTempFile(PDFMergerUtility merger) throws IOException {
        Path merged = Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(merged))) {
            merger.setDestinationStream(output);
            merger.mergeDocuments(IOUtils.createTempFileOnlyStreamCache());
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(merged);
            throw e;
        }
        return merged;
    }
}
