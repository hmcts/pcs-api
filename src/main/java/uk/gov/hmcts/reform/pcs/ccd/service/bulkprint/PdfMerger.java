package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Merges the coversheet and pack documents into a single PDF, in order (coversheet first). Sending one
 * combined document — rather than a separate coversheet followed by the pack — means Send Letter's
 * per-document duplicate checksum reflects the whole letter, so genuinely different packs to the same
 * recipient (claim / defence / counter-claim) no longer collide on the identical coversheet, while an
 * exact retry still produces identical bytes and is de-duplicated.
 */
@Service
public class PdfMerger {

    public byte[] merge(List<byte[]> pdfs) {
        try (PDDocument merged = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFMergerUtility mergerUtility = new PDFMergerUtility();
            for (byte[] pdf : pdfs) {
                try (PDDocument document = Loader.loadPDF(pdf)) {
                    mergerUtility.appendDocument(merged, document);
                }
            }
            merged.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new BulkPrintMergeException("Failed to merge bulk-print PDFs", e);
        }
    }
}
