package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.gov.hmcts.ccd.sdk.conversion.ConversionException;
import uk.gov.hmcts.ccd.sdk.conversion.DocumentConverter;
import uk.gov.hmcts.ccd.sdk.conversion.docmosis.DocmosisRenderService;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the real SDK converter: only Docmosis is stubbed. What comes out is exactly what is posted to Send Letter.
 */
class BulkPrintLetterAssemblerTest {

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final float MM = 72f / 25.4f;

    @TempDir
    private Path tempDir;

    private final List<String> docmosisCalls = new CopyOnWriteArrayList<>();

    @Test
    @DisplayName("Keeps the coversheet as rendered and makes every document print-ready in one even-page PDF")
    void shouldBuildOnePrintReadyLetterBehindTheCoversheet() throws IOException {
        byte[] coversheet = pdfSaying("Jane Doe, 1 High Street, London W1 1AA");
        BulkPrintLetterAssembler underTest = new BulkPrintLetterAssembler(converterWithDocmosis());

        byte[] letter = underTest.assemble("CPC-01-IN0 1234-5678-9012-3456", coversheet, List.of(
            new BulkPrintLetterAssembler.LetterDocument("photo.png", "image/png", png()),
            new BulkPrintLetterAssembler.LetterDocument("witness-statement.docx", DOCX, "docx".getBytes()),
            new BulkPrintLetterAssembler.LetterDocument("rent-schedule.pdf", "application/pdf",
                pdfSaying("Rent schedule right at the edge"))));

        assertThat(new String(letter, 0, 8, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-1.3");
        try (PDDocument document = Loader.loadPDF(letter)) {
            // coversheet + photo + statement + schedule = 4: already even, so no padding page.
            assertThat(document.getNumberOfPages()).isEqualTo(4);
            assertThat(document.getDocumentInformation().getCustomMetadataValue("GTS_PDFXConformance"))
                .isEqualTo("PDF/X-1a:2001");
            assertThat(textOf(document, 1)).contains("Jane Doe, 1 High Street, London W1 1AA");
            for (int page = 2; page <= 4; page++) {
                // Converted pages are rendered images: no live text, nothing inside the 20 mm margin.
                assertThat(textOf(document, page)).isBlank();
                assertMarginBlank(document, page - 1);
            }
        }
        assertThat(docmosisCalls).containsExactly("witness-statement.docx " + DOCX);
    }

    @Test
    @DisplayName("Pads an odd page count so Send Letter forwards the letter untouched")
    void shouldPadToAnEvenPageCount() throws IOException {
        BulkPrintLetterAssembler underTest = new BulkPrintLetterAssembler(converterWithDocmosis());

        byte[] letter = underTest.assemble("DEF-01-IN0 1234-5678-9012-3456", pdfSaying("Coversheet"), List.of(
            new BulkPrintLetterAssembler.LetterDocument("defence.pdf", "application/pdf", pdfSaying("Defence")),
            new BulkPrintLetterAssembler.LetterDocument("photo.png", "image/png", png())));

        try (PDDocument document = Loader.loadPDF(letter)) {
            assertThat(document.getNumberOfPages()).isEqualTo(4);
        }
    }

    @Test
    @DisplayName("Fails the pack with the conversion reason when a document cannot be converted")
    void shouldThrowMergeExceptionWhenADocumentCannotBeConverted() throws IOException {
        BulkPrintLetterAssembler underTest = new BulkPrintLetterAssembler(
            DocumentConverter.builder().tempDirectory(tempDir).build());
        byte[] coversheet = pdfSaying("Coversheet");

        assertThatThrownBy(() -> underTest.assemble("GEN-00-IN0 1234-5678-9012-3456", coversheet, List.of(
            new BulkPrintLetterAssembler.LetterDocument("statement.docx", DOCX, "docx".getBytes()))))
            .isInstanceOf(BulkPrintMergeException.class)
            .hasMessageContaining("statement.docx")
            .hasCauseInstanceOf(ConversionException.class);
    }

    private DocumentConverter converterWithDocmosis() {
        DocmosisRenderService docmosis = (source, fileName, mediaType) -> {
            docmosisCalls.add(fileName + " " + mediaType);
            try {
                return Files.write(Files.createTempFile(tempDir, "docmosis-", ".pdf"),
                    pdfSaying("Converted " + fileName));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
        return DocumentConverter.builder().docmosis(docmosis).tempDirectory(tempDir).build();
    }

    private static String textOf(PDDocument document, int page) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        return stripper.getText(document);
    }

    private static void assertMarginBlank(PDDocument document, int pageIndex) throws IOException {
        float dpi = 50;
        BufferedImage page = new PDFRenderer(document)
            .renderImageWithDPI(pageIndex, dpi, ImageType.GRAY);
        int band = (int) Math.floor(20 * MM / 72 * dpi) - 1;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                boolean inMargin = x < band || y < band || x >= page.getWidth() - band || y >= page.getHeight() - band;
                if (inMargin) {
                    assertThat(page.getRGB(x, y) & 0xFF).as("ink within 20mm on page %d", pageIndex + 1)
                        .isGreaterThanOrEqualTo(250);
                }
            }
        }
    }

    private static byte[] pdfSaying(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contents = new PDPageContentStream(document, page)) {
                contents.setNonStrokingColor(0.9f, 0.1f, 0.1f);
                contents.addRect(0, 0, PDRectangle.A4.getWidth(), 40);
                contents.fill();
                contents.beginText();
                contents.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contents.newLineAtOffset(5, PDRectangle.A4.getHeight() - 15);
                contents.showText(text);
                contents.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(20, 120, 200));
        g.fillOval(100, 100, 1000, 700);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
