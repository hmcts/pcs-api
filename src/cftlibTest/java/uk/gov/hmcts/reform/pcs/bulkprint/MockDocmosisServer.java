package uk.gov.hmcts.reform.pcs.bulkprint;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;

/**
 * Stands in for the shared Docmosis service's {@code /rs/convert}: every office document comes back as a one-page
 * PDF, so the rest of the bulk print conversion runs for real.
 */
@Component
@RequiredArgsConstructor
public class MockDocmosisServer {

    private static final String CONVERT_PATH = "/rs/convert";

    private final WireMockServer wireMockServer;

    @PostConstruct
    public void stubConvert() {
        wireMockServer.stubFor(WireMock.post(urlPathEqualTo(CONVERT_PATH))
            .willReturn(WireMock.ok()
                .withHeader("Content-Type", "application/pdf")
                .withBody(convertedPdf())));
    }

    /** The conversion requests Docmosis received, as multipart bodies. */
    public List<String> convertRequests() {
        return wireMockServer.findAll(RequestPatternBuilder.newRequestPattern(POST, urlPathEqualTo(CONVERT_PATH)))
            .stream()
            .map(LoggedRequest::getBodyAsString)
            .toList();
    }

    private static byte[] convertedPdf() {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(36, 800);
                content.showText("Converted by the Docmosis stub, right up to the edge of the page");
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
