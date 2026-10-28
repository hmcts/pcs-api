package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.conversion.ConversionException;
import uk.gov.hmcts.ccd.sdk.conversion.ConversionSource;
import uk.gov.hmcts.ccd.sdk.conversion.ConvertedPdf;
import uk.gov.hmcts.ccd.sdk.conversion.DocumentConverter;
import uk.gov.hmcts.ccd.sdk.conversion.PrintOptions;
import uk.gov.hmcts.ccd.sdk.conversion.PrintPart;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Builds the single PDF a pack is posted as: the coversheet exactly as rendered, then each document converted to
 * the bulk print standard (PDF/X-1a pages, A4, images at most 300 dpi, nothing within 20 mm of an edge), padded to an
 * even page count. Documents may be any type Possessions accepts as evidence (Word, Excel, PowerPoint, images,
 * text, PDF); office formats go through Docmosis.
 *
 * <p>One even-page document is what Send Letter forwards to Xerox untouched; it re-saves anything else as PDF 1.6,
 * which would undo the print standard. One document also means Send Letter's duplicate check covers the whole
 * letter, so genuinely different packs to the same recipient (claim / defence / counter-claim) never collide on
 * the identical coversheet, while an exact retry still matches and is de-duplicated.
 */
@Service
public class BulkPrintLetterAssembler {

    private static final FileAttribute<Set<PosixFilePermission>> OWNER_ONLY =
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));

    private final DocumentConverter documentConverter;
    private final PrintOptions printOptions = PrintOptions.bulkPrint();

    public BulkPrintLetterAssembler(DocumentConverter documentConverter) {
        this.documentConverter = documentConverter;
    }

    public byte[] assemble(String title, byte[] coversheet, List<LetterDocument> documents) {
        Path directory = null;
        try {
            // Owner-only: the directory holds case documents, and the temp root is shared.
            directory = Files.createTempDirectory("pcs-bulk-print-", OWNER_ONLY);
            List<PrintPart> parts = new ArrayList<>();
            parts.add(PrintPart.asIs(Files.write(directory.resolve("coversheet.pdf"), coversheet), "coversheet"));
            for (int i = 0; i < documents.size(); i++) {
                LetterDocument document = documents.get(i);
                Path file = Files.write(directory.resolve("document-" + i), document.content());
                parts.add(PrintPart.convert(
                    ConversionSource.of(file, nameOf(document, i), document.contentType())));
            }
            try (ConvertedPdf letter = documentConverter.toPrintReadyLetter(title, parts, printOptions)) {
                return Files.readAllBytes(letter.file());
            }
        } catch (ConversionException e) {
            throw new BulkPrintMergeException(
                "Failed to convert bulk-print letter " + title + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new BulkPrintMergeException("Failed to assemble bulk-print letter " + title, e);
        } finally {
            deleteQuietly(directory);
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
            return other instanceof LetterDocument that
                && Objects.equals(fileName, that.fileName)
                && Objects.equals(contentType, that.contentType)
                && Arrays.equals(content, that.content);
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

    private static void deleteQuietly(Path directory) {
        if (directory == null) {
            return;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            // Best effort: the temporary directory is cleared with the pod's temp space.
        }
    }
}
