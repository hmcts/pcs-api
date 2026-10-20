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
 * Merges into temp files rather than into memory, so a batch print's size does not bound the heap.
 */
@Component
public class BatchPrintPdfMerger {

    private static final String CASE_FILE_PREFIX = "case-";
    private static final String BATCH_FILE_PREFIX = "batch-";
    private static final String TEMP_FILE_SUFFIX = ".pdf";

    public Path createMergeDirectory() throws IOException {
        return BatchPrintTempDirectory.create();
    }

    public Path mergeCase(Path directory, List<byte[]> documents) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        documents.forEach(document -> merger.addSource(new RandomAccessReadBuffer(document)));
        return mergeToTempFile(directory, CASE_FILE_PREFIX, merger);
    }

    public Path mergeCases(Path directory, List<Path> caseFiles) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        for (Path caseFile : caseFiles) {
            merger.addSource(caseFile.toFile());
        }
        return mergeToTempFile(directory, BATCH_FILE_PREFIX, merger);
    }

    private static Path mergeToTempFile(Path directory, String prefix, PDFMergerUtility merger)
        throws IOException {
        Path merged = Files.createTempFile(directory, prefix, TEMP_FILE_SUFFIX,
                                           BatchPrintTempDirectory.OWNER_ONLY_FILE);
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
