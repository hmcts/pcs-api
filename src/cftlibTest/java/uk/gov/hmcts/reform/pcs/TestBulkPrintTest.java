package uk.gov.hmcts.reform.pcs;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.bulkprint.MockDocmosisServer;
import uk.gov.hmcts.reform.pcs.bulkprint.MockSendLetterServer;
import uk.gov.hmcts.reform.pcs.bulkprint.model.Letter;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.client.TestingSupportClient;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.MockDocumentStore;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.testBulkPrint;
import static uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType.TEST_SUPPORT_PACK;
import static uk.gov.hmcts.reform.pcs.testingsupport.bulkprint.TestBulkPrintTaskComponent.TEST_BULK_PRINT_TASK_NAME;

/**
 * The test support bulk print event end to end: documents of several types are uploaded to a case, the event posts
 * them, and the letter Send Letter receives must be one PDF that meets the bulk print standard. Everything between
 * runs for real (CCD, CDAM, the coversheet, the conversion and the Send Letter client); only the document store,
 * Docmosis and Send Letter are stubbed.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestBulkPrintTest extends CftlibTest {

    private static final float MM = 72f / 25.4f;
    // The shared document-store Wiremock serves this for the coversheet, which doc assembly renders.
    private static final Path COVERSHEET = Path.of("wiremock/__files/dm-store/placeholder.pdf");

    @Autowired
    private CcdClient ccdClient;
    @Autowired
    private IdamClient idamClient;
    @Autowired
    private TestingSupportClient testingSupportClient;
    @Autowired
    private MockSendLetterServer mockSendLetterServer;
    @Autowired
    private MockDocmosisServer mockDocmosisServer;
    @Autowired
    private MockDocumentStore mockDocumentStore;

    private String solicitorToken;

    @BeforeAll
    void setup() {
        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
    }

    @AfterAll
    void removeStoredDocuments() {
        mockDocumentStore.removeStoredDocuments();
    }

    @Test
    void postsUploadedDocumentsOfEveryKindAsOnePrintReadyLetter() throws IOException {
        long caseReference = createCaseWithClaimant();
        final int docmosisCallsBefore = mockDocmosisServer.convertRequests().size();

        List<ListValue<Document>> uploads = List.of(
            upload("photo.jpg", "image/jpeg", photo("jpeg")),
            upload("scan.png", "image/png", photo("png")),
            upload("statement.pdf", "application/pdf", pdfWithALandscapePage()),
            upload("witness-statement.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "stand-in for a Word document".getBytes(StandardCharsets.UTF_8)),
            upload("rent-schedule.csv", "text/csv",
                "date,amount\n2026-01-01,450.00\n".getBytes(StandardCharsets.UTF_8)));
        ccdClient.updateCase(testBulkPrint, caseReference,
            PCSCase.builder().testBulkPrintDocuments(uploads).build(), solicitorToken);
        testingSupportClient.executeFutureDbSchedulerTasks(TEST_BULK_PRINT_TASK_NAME, Duration.ofMinutes(1));

        Letter letter = mockSendLetterServer.getSentLetters(caseReference, TEST_SUPPORT_PACK, 1).getFirst();
        assertThat(letter.documents).as("one PDF, so Send Letter forwards it unchanged").hasSize(1);
        byte[] pdf = Base64.getDecoder().decode(letter.documents.getFirst().content);

        int coversheetPages = pageCount(Files.readAllBytes(COVERSHEET));
        // photo, scan, two statement pages, Word document, CSV
        int convertedPages = 6;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            int pages = document.getNumberOfPages();
            assertThat(pages % 2).as("page count %d is even for duplex", pages).isZero();
            assertThat(pages).isIn(coversheetPages + convertedPages, coversheetPages + convertedPages + 1);
            for (int index = coversheetPages; index < coversheetPages + convertedPages; index++) {
                assertPrintReady(document, index);
            }
        }
        assertThat(mockDocmosisServer.convertRequests().subList(docmosisCallsBefore,
                mockDocmosisServer.convertRequests().size()))
            .as("office documents go to Docmosis with the access key")
            .hasSize(2)
            .allSatisfy(body -> assertThat(body).contains("cftlib-docmosis-key"))
            .anySatisfy(body -> assertThat(body).contains("witness-statement.docx"))
            .anySatisfy(body -> assertThat(body).contains("rent-schedule.csv"));
    }

    /** A converted page is A4 and holds exactly one CMYK image, no live text, and nothing within 20 mm of an edge. */
    private static void assertPrintReady(PDDocument document, int index) throws IOException {
        String where = "page " + (index + 1);
        PDPage page = document.getPage(index);
        assertThat(page.getMediaBox().getWidth()).as(where + " width").isCloseTo(PDRectangle.A4.getWidth(),
            org.assertj.core.data.Offset.offset(1f));
        assertThat(page.getMediaBox().getHeight()).as(where + " height").isCloseTo(PDRectangle.A4.getHeight(),
            org.assertj.core.data.Offset.offset(1f));

        PDResources resources = page.getResources();
        List<String> colourSpaces = new ArrayList<>();
        for (COSName name : resources.getXObjectNames()) {
            if (resources.getXObject(name) instanceof PDImageXObject image) {
                colourSpaces.add(image.getColorSpace().getName());
            }
        }
        assertThat(colourSpaces).as(where + " images").containsExactly("DeviceCMYK");

        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(index + 1);
        stripper.setEndPage(index + 1);
        assertThat(stripper.getText(document)).as(where + " live text").isBlank();

        float dpi = 50;
        BufferedImage render = new PDFRenderer(document).renderImageWithDPI(index, dpi, ImageType.GRAY);
        int band = (int) Math.floor(20 * MM / 72 * dpi);
        for (int y = 0; y < render.getHeight(); y++) {
            for (int x = 0; x < render.getWidth(); x++) {
                boolean inMargin = x < band || y < band
                    || x >= render.getWidth() - band || y >= render.getHeight() - band;
                if (inMargin) {
                    assertThat(render.getRGB(x, y) & 0xFF).as("%s ink at (%d, %d) within 20 mm", where, x, y)
                        .isGreaterThanOrEqualTo(250);
                }
            }
        }
    }

    private ListValue<Document> upload(String fileName, String mimeType, byte[] content) {
        UUID documentId = mockDocumentStore.store(content, mimeType);
        String url = mockDocumentStore.url(documentId);
        return ListValue.<Document>builder()
            .id(UUID.randomUUID().toString())
            .value(Document.builder().url(url).binaryUrl(url + "/binary").filename(fileName).build())
            .build();
    }

    private long createCaseWithClaimant() {
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Baker Street")
            .postTown("London")
            .postCode("NW1 6XE")
            .build();
        CaseDetails created = ccdClient.createCase(PCSCase.builder()
            .propertyAddress(address)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build(), solicitorToken);
        long caseReference = created.getId();
        ccdClient.updateCase(resumePossessionClaim, caseReference, PCSCase.builder()
            .caseManagementLocationNumber(20262)
            .regionId(1)
            .claimantInformation(ClaimantInformation.builder()
                .claimantName("Bulk Print Test Claimant")
                .isClaimantNameCorrect(VerticalYesNo.YES)
                .build())
            .claimantContactPreferences(ClaimantContactPreferences.builder()
                .overriddenClaimantContactAddress(address)
                .build())
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                .build())
            .defendant1(DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("Danny")
                .lastName("Defendant")
                .addressKnown(VerticalYesNo.YES)
                .correspondenceAddress(address)
                .build())
            .noticeServed(YesOrNo.NO)
            .languageUsed(LanguageUsed.ENGLISH)
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .build(), solicitorToken);
        return caseReference;
    }

    /** A landscape colour photo, so the conversion has to turn it, scale it and separate it into CMYK. */
    private static byte[] photo(String format) throws IOException {
        BufferedImage image = new BufferedImage(1600, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(40, 120, 200));
            g.fillRect(0, 0, 1600, 1200);
            g.setColor(Color.ORANGE);
            g.fillOval(400, 200, 800, 800);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    /** Two pages with text up to the edges, the second landscape. */
    private static byte[] pdfWithALandscapePage() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (PDRectangle size : List.of(PDRectangle.A4,
                new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()))) {
                PDPage page = new PDPage(size);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(2, size.getHeight() - 14);
                    content.showText("Statement text printed right at the top-left corner of the page");
                    content.endText();
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static int pageCount(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }
}
