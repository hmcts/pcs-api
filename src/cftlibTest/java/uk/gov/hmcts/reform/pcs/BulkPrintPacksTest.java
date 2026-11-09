package uk.gov.hmcts.reform.pcs;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jose4j.base64url.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.bulkprint.MockSendLetterServer;
import uk.gov.hmcts.reform.pcs.bulkprint.model.Letter;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page.RespondToPossessionDraftSavePage;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.client.TestingSupportClient;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.model.FeePaymentSummary;
import uk.gov.hmcts.reform.pcs.model.PartyAccessCode;
import uk.gov.hmcts.reform.pcs.service.AccessCodeService;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.reform.pcs.service.CaseDocumentService;
import uk.gov.hmcts.reform.pcs.service.DocumentServiceStubs;
import uk.gov.hmcts.reform.pcs.service.FeePaymentService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType.PAYMENT_OR_COMPENSATION;
import static uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType.CLAIMANT_CLAIM_PACK;
import static uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType.DEFENCE_PACK;
import static uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType.DEFENDANT_CLAIM_PACK;
import static uk.gov.hmcts.reform.pcs.ccd.task.BulkPrintScheduledTask.BULK_PRINT_TASK_NAME;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class BulkPrintPacksTest extends CftlibTest {

    private static final long INITIAL_DRAFT_VERSION = 0L;
    @Autowired
    private CcdClient ccdClient;
    @Autowired
    private CaseCreationService caseCreationService;
    @Autowired
    private IdamClient idamClient;
    @Autowired
    private CaseDocumentService caseDocumentService;
    @Autowired
    private AccessCodeService accessCodeService;
    @Autowired
    private FeePaymentService feePaymentService;
    @Autowired
    private TestingSupportClient testingSupportClient;
    @Autowired
    private MockSendLetterServer mockSendLetterServer;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DocumentServiceStubs documentServiceStubs;

    private String solicitorToken;
    private String defendant1Token;
    private String defendant2Token;

    private Long caseReference;

    @BeforeAll
    void setup() {
        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
        defendant1Token = idamClient.getAccessToken("defendant1@localhost", "password");
        defendant2Token = idamClient.getAccessToken("defendant2@localhost", "password");
    }

    @Test
    @Order(1)
    void verifyClaimPacks() {
        Instant testStart = Instant.now();

        caseReference = caseCreationService.createAndIssueCase(solicitorToken, LanguageUsed.ENGLISH);

        caseDocumentService.waitForDocumentsOfType(caseReference, DocumentType.CLAIM, testStart, 1);
        // Case-attached metadata makes CDAM check that CCD exposes each document to PCS's system user.
        List<UUID> claimForms = caseDocumentService.waitForDocumentIds(caseReference, DocumentType.CLAIM, 1);
        List<UUID> accessCodeLetters =
            caseDocumentService.waitForDocumentIds(caseReference, DocumentType.DEFENDANT_ACCESS_CODE, 2);
        List<StubMapping> attached = Stream.concat(claimForms.stream(), accessCodeLetters.stream())
            .map(documentId -> documentServiceStubs.attachToCase(documentId, caseReference))
            .toList();
        try {
            testingSupportClient.executeFutureDbSchedulerTasks(BULK_PRINT_TASK_NAME, Duration.ofDays(2));

            List<Letter> defendantLetters = mockSendLetterServer.getSentLetters(caseReference, DEFENDANT_CLAIM_PACK,
                                                                                testStart,
                                                                                2
            );
            assertThat(defendantLetters)
                .map(BulkPrintPacksTest::getRecipientName)
                .containsExactlyInAnyOrder("Danny Defendant", "Delia Defendant");

            // Rely on the fact that claimant letters would be sent before defendant letters, so
            // we can assert that none have been sent without waiting any longer here.
            List<Letter> claimantLetters = mockSendLetterServer.getSentLetters(caseReference, CLAIMANT_CLAIM_PACK,
                                                                               testStart,
                                                                               0
            );
            assertThat(claimantLetters).isEmpty();
            assertThat(attached)
                .as("CDAM checked each pack document's access through CCD")
                .allSatisfy(stub -> assertThat(documentServiceStubs.requestsServedBy(stub)).isPositive());
        } finally {
            attached.forEach(documentServiceStubs::remove);
        }
    }

    @Test
    @Order(2)
    void verifyResponsePackNoCounterClaim() throws IOException {
        Instant testStart = Instant.now();

        List<PartyAccessCode> partyAccessCodes = accessCodeService.waitForAccessCodes(caseReference, 2);
        PartyAccessCode partyAccessCode1 = partyAccessCodes.getFirst();
        accessCodeService.linkUserToCase(caseReference, partyAccessCode1.getAccessCode(), defendant1Token);

        String emailAddress = "defendant1@cftlib.test.com";

        submitDefendantResponse(emailAddress, null, defendant1Token);

        caseDocumentService.waitForDocumentsOfType(caseReference, DocumentType.DEFENDANT_RESPONSE, testStart, 1);

        testingSupportClient.executeFutureDbSchedulerTasks(BULK_PRINT_TASK_NAME, Duration.ofDays(2));

        List<Letter> letters = mockSendLetterServer.getSentLetters(caseReference, DEFENCE_PACK, testStart, 2);
        assertThat(letters)
            .map(BulkPrintPacksTest::getRecipientName)
            .containsExactlyInAnyOrder("Danny Defendant", "Delia Defendant");

        try (PDDocument pdDocument = findPackPdf("Danny Defendant", letters)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(2);
            // Coversheet is posted as-is; converted pack pages have no extractable text.
            assertThat(getPageContents(pdDocument, 0)).contains("Template: CV-PCS-LET-ENG-Coversheet.docx");
            assertConvertedPage(pdDocument, 1);
        }

        try (PDDocument pdDocument = findPackPdf("Delia Defendant", letters)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(2);
            assertThat(getPageContents(pdDocument, 0)).contains("Template: CV-PCS-LET-ENG-Coversheet.docx");
            assertConvertedPage(pdDocument, 1);
        }

    }

    @Test
    @Order(3)
    void verifyResponsePackWithCounterClaim() throws IOException {
        final Instant testStart = Instant.now();

        List<PartyAccessCode> partyAccessCodes = accessCodeService.waitForAccessCodes(caseReference, 2);
        PartyAccessCode partyAccessCode2 = partyAccessCodes.get(1);
        accessCodeService.linkUserToCase(caseReference, partyAccessCode2.getAccessCode(), defendant2Token);

        String emailAddress = "defendant2@cftlib.test.com";

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(new BigDecimal("123.45"))
            .build();

        submitDefendantResponse(emailAddress, counterClaim, defendant2Token);

        payForCounterclaim();

        caseDocumentService.waitForDocumentsOfType(caseReference, DocumentType.DEFENDANT_RESPONSE, testStart, 1);
        caseDocumentService.waitForDocumentsOfType(caseReference, DocumentType.COUNTERCLAIM, testStart, 1);

        testingSupportClient.executeFutureDbSchedulerTasks(BULK_PRINT_TASK_NAME, Duration.ofDays(2));

        List<Letter> letters = mockSendLetterServer.getSentLetters(caseReference, DEFENCE_PACK, testStart, 2);
        assertThat(letters)
            .map(BulkPrintPacksTest::getRecipientName)
            .containsExactlyInAnyOrder("Danny Defendant", "Delia Defendant");

        try (PDDocument pdDocument = findPackPdf("Danny Defendant", letters)) {
            // Coversheet + defence + counterclaim is 3 pages; bulk print pads to an even count for duplex.
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(4);
            assertThat(getPageContents(pdDocument, 0)).contains("Template: CV-PCS-LET-ENG-Coversheet.docx");
            assertConvertedPage(pdDocument, 1);
            assertConvertedPage(pdDocument, 2);
        }

        try (PDDocument pdDocument = findPackPdf("Delia Defendant", letters)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(4);
            assertThat(getPageContents(pdDocument, 0)).contains("Template: CV-PCS-LET-ENG-Coversheet.docx");
            assertConvertedPage(pdDocument, 1);
            assertConvertedPage(pdDocument, 2);
        }

    }

    private void submitDefendantResponse(String emailAddress, CounterClaim counterClaim, String defendantToken) {
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByPost(VerticalYesNo.YES)
            .defendantNameConfirmation(VerticalYesNo.YES)
            .propertyAddressConfirmation(VerticalYesNo.YES)
            .correspondenceAddressConfirmation(VerticalYesNo.YES)
            .counterClaim(counterClaim)
            .build();

        DefendantContactDetails defendantContactDetails = DefendantContactDetails.builder()
            .party(Party.builder()
                       .emailAddress(emailAddress)
                       .build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                                         .defendantResponses(defendantResponses)
                                         .defendantContactDetails(defendantContactDetails)
                                         .draftVersion(INITIAL_DRAFT_VERSION)
                                         .build())
            .build();

        StartEventResponse startEventResponse = ccdClient.startEvent(
            EventId.respondPossessionClaim,
            caseReference,
            defendantToken
        );

        CaseDetails midEventResponseDetails = ccdClient.midEvent(
            EventId.respondPossessionClaim,
            caseReference,
            EventId.respondPossessionClaim.name() + RespondToPossessionDraftSavePage.PAGE_ID,
            caseData,
            startEventResponse.getToken(),
            defendantToken
        );

        long draftVersion = objectMapper.convertValue(midEventResponseDetails.getData(), PCSCase.class)
            .getPossessionClaimResponse()
            .getDraftVersion();

        PCSCase submissionCaseData = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder().draftVersion(draftVersion).build())
            .build();

        ccdClient.submitEvent(
            EventId.respondPossessionClaim,
            caseReference,
            submissionCaseData,
            startEventResponse.getToken(),
            defendantToken
        );
    }

    private void payForCounterclaim() {
        List<FeePaymentSummary> feePaymentSummaries = feePaymentService
            .waitForFeePaymentRequests(caseReference, PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE);

        feePaymentService.simulatePayments(caseReference, feePaymentSummaries);
    }


    private static PDDocument findPackPdf(String recipientName, List<Letter> letters) throws IOException {
        Letter packLetter = findLetterSentTo(recipientName, letters);
        return Loader.loadPDF(Base64.decode(packLetter.documents.getFirst().content));
    }

    private static String getPageContents(PDDocument pdDocument, int pageIndex) throws IOException {
        try (InputStream inputStream = pdDocument.getPage(pageIndex).getContents()) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    /** Pack documents are rasterised; full print-ready checks live in TestBulkPrintTest. */
    private static void assertConvertedPage(PDDocument document, int index) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(index + 1);
        stripper.setEndPage(index + 1);
        assertThat(stripper.getText(document)).as("page " + (index + 1) + " live text").isBlank();
    }

    private static Letter findLetterSentTo(String recipientName, List<Letter> letters) {
        return letters.stream()
            .filter(letter -> recipientName.equals(letter.additionalData.getRecipients().getFirst()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No letter found for " + recipientName));
    }


    private static String getRecipientName(Letter letter) {
        return letter.additionalData.getRecipients().getFirst();
    }

}
