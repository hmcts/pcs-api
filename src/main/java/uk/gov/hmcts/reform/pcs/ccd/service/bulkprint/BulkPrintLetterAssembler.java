package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocSource;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocToolkit;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocToolkitException;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PdfPart;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PdfRequest;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PdfResult;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PrintOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Builds the single PDF a pack is posted as: the coversheet exactly as rendered, then each document converted to
 * the bulk print standard (PDF/X-1a pages, A4, images at most 300 dpi, nothing within 20 mm of an edge). Each part
 * is padded to an even page count, as Send Letter pads each document it merges, so every document starts on a new
 * sheet rather than on the back of the coversheet or the document before. Documents may be any type Possessions
 * accepts as evidence (Word, Excel, PowerPoint, images, text, PDF); office formats go through Docmosis.
 *
 * <p>One even-page document is what Send Letter forwards to Xerox untouched; it re-saves anything else as PDF 1.6,
 * which would undo the print standard. One document also means Send Letter's duplicate check covers the whole
 * letter, so genuinely different packs to the same recipient (claim / defence / counter-claim) never collide on
 * the identical coversheet, while an exact retry still matches and is de-duplicated.
 */
@Service
public class BulkPrintLetterAssembler {

    /**
     * Above the SDK's 20 MiB default: every page is rasterised, so a pack of several documents can pass 20 MiB
     * even when the uploads are small. Send Letter has no request size limit of its own.
     */
    static final long MAX_LETTER_BYTES = 50L * 1024 * 1024;

    /**
     * Below the SDK's 300 dpi default (Xerox accepts up to 300). Every page is stored as a lossless image, so 200 dpi
     * roughly halves the size of a pack with no visible loss of legibility, and keeps evidence-heavy packs under
     * {@link #MAX_LETTER_BYTES}.
     */
    static final int LETTER_DPI = 200;

    private final DocToolkit docToolkit;
    private final PrintOptions printOptions = PrintOptions.bulkPrint().toBuilder()
        .maxOutputBytes(MAX_LETTER_BYTES)
        .resolutionDpi(LETTER_DPI)
        .build();

    public BulkPrintLetterAssembler(DocToolkit docToolkit) {
        this.docToolkit = docToolkit;
    }

    public byte[] assemble(String title, byte[] coversheet, List<LetterDocument> documents) {
        PdfRequest.Builder request = PdfRequest.builder(title)
            .document(PdfPart.builder(DocSource.bytes(coversheet, "coversheet.pdf")).unchanged().build())
            .print(printOptions);
        for (int i = 0; i < documents.size(); i++) {
            LetterDocument document = documents.get(i);
            request.document(DocSource.bytes(document.content(), nameOf(document, i), document.contentType()));
        }
        try (PdfResult letter = docToolkit.render(request.build())) {
            return Files.readAllBytes(letter.file());
        } catch (DocToolkitException e) {
            throw new BulkPrintMergeException(
                "Failed to convert bulk-print letter " + title + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new BulkPrintMergeException("Failed to assemble bulk-print letter " + title, e);
        }
    }

    /**
     * The name the converter reports the document by. Documents PCS generates itself, such as the defendant access
     * code letter, can be stored without one; their type is then detected from the content.
     */
    private static String nameOf(LetterDocument document, int index) {
        String fileName = document.fileName();
        return fileName == null || fileName.isBlank() ? "document-" + (index + 1) : fileName;
    }

    /**
     * One document of a pack, as fetched from the document store.
     *
     * @param fileName the stored file name; may be missing for documents PCS generated
     * @param contentType the stored media type; null or wrong is tolerated, the content decides
     */
    public record LetterDocument(String fileName, String contentType, byte[] content) {

        @Override
        public boolean equals(Object other) {
            return other instanceof LetterDocument(String otherName, String otherType, byte[] otherContent)
                && Objects.equals(fileName, otherName)
                && Objects.equals(contentType, otherType)
                && Arrays.equals(content, otherContent);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(fileName, contentType) + Arrays.hashCode(content);
        }

        @Override
        public String toString() {
            return "LetterDocument[fileName=" + fileName + ", contentType=" + contentType + ", "
                + (content == null ? 0 : content.length) + " bytes]";
        }
    }
}
