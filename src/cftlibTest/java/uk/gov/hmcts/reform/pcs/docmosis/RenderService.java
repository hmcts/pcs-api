package uk.gov.hmcts.reform.pcs.docmosis;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class RenderService {

    /**
     * Basic rendering of a PDF with the template name and data written into
     * the page content for debugging purposes. Note that the content
     * does not wrap so some of the data goes off the page, but it is enough
     * to get a general idea
     * @param renderRequest The template and data to render
     * @return A single page PDF in a ByteArrayOutputStream
     */
    public ByteArrayOutputStream renderTemplate(RenderRequest renderRequest) {
        log.info("Creating placeholder PDF for %s with output name %s"
                     .formatted(renderRequest.getTemplateName(), renderRequest.getOutputName()));

        PDDocument document = new PDDocument();

        PDPage page = new PDPage();
        document.addPage(page);

        try {
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PDRectangle mediaBox = page.getMediaBox();

            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD), 16);
            contentStream.newLineAtOffset(60, mediaBox.getUpperRightY() - 100);
            contentStream.showText(renderRequest.getOutputName());

            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 12);
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Template: " + renderRequest.getTemplateName());
            contentStream.newLineAtOffset(0, -30);
            contentStream.showText("Data:");
            contentStream.newLineAtOffset(10, -20);
            renderRequest.getData().forEach(
                    (key, value) -> {
                        try {
                            String renderValue = String.valueOf(value)
                                    .replaceAll("\n", " ")
                                    .replaceAll("\r", " ");
                            contentStream.showText("%s: %s".formatted(key, renderValue));
                            contentStream.newLineAtOffset(0, -18);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );

            contentStream.endText();

            contentStream.close();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            document.close();

            return outputStream;
        } catch (IOException ioException) {
            throw new RenderException("Failed to render template", ioException);
        }

    }

}
