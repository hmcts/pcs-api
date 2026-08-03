package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfMergerTest {

    private final PdfMerger underTest = new PdfMerger();

    @Test
    @DisplayName("Merges PDFs in order into a single document with the combined page count")
    void shouldMergePdfsInOrder() throws IOException {
        byte[] coversheet = pdf(1);
        byte[] claimForm = pdf(3);

        byte[] merged = underTest.merge(List.of(coversheet, claimForm));

        try (PDDocument document = Loader.loadPDF(merged)) {
            assertThat(document.getNumberOfPages()).isEqualTo(4);
        }
    }

    @Test
    @DisplayName("Throws BulkPrintMergeException when a source PDF is not valid")
    void shouldThrowWhenSourcePdfIsInvalid() {
        assertThatThrownBy(() -> underTest.merge(List.of("not a pdf".getBytes())))
            .isInstanceOf(BulkPrintMergeException.class);
    }

    private byte[] pdf(int pages) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                document.addPage(new PDPage());
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
