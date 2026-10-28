package uk.gov.hmcts.reform.pcs;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
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
import uk.gov.hmcts.reform.pcs.model.PartyAccessCode;
import uk.gov.hmcts.reform.pcs.service.AccessCodeService;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.reform.pcs.service.CaseDocumentService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType.PAYMENT_OR_COMPENSATION;
import static uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType.CLAIMANT_CLAIM_PACK;
import static uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType.DEFENCE_PACK;
import static uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType.DEFENDANT_CLAIM_PACK;
import static uk.gov.hmcts.reform.pcs.ccd.task.BulkPrintScheduledTask.BULK_PRINT_TASK_NAME;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class PostAndEmailNotificationsTest extends CftlibTest {

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
    private TestingSupportClient testingSupportClient;
    @Autowired
    private MockSendLetterServer mockSendLetterServer;
    @Autowired
    private ObjectMapper objectMapper;

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
        testingSupportClient.executeFutureDbSchedulerTasks(BULK_PRINT_TASK_NAME, Duration.ofDays(2));

        List<Letter> claimantLetters = mockSendLetterServer.getSentLetters(caseReference, CLAIMANT_CLAIM_PACK,
                                                                           testStart,
                                                                           1
        );
        Letter claimantLetter = claimantLetters.getFirst();
        assertThat(getRecipientName(claimantLetter)).isEqualTo("CftLibTest Claimant");

        List<Letter> defendantLetters = mockSendLetterServer.getSentLetters(caseReference, DEFENDANT_CLAIM_PACK,
                                                                            testStart,
                                                                            2
        );
        assertThat(defendantLetters)
            .map(PostAndEmailNotificationsTest::getRecipientName)
            .containsExactlyInAnyOrder("Danny Defendant", "Delia Defendant");
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
            .map(PostAndEmailNotificationsTest::getRecipientName)
            .containsExactlyInAnyOrder("Danny Defendant", "Delia Defendant");

        try (PDDocument pdDocument = findPackPdf("Danny Defendant", letters)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(2);
            // The fake document renderer writes the template name into the PDF page
            assertThat(getPageContents(pdDocument, 0)).contains("Template: CV-PCS-LET-ENG-Coversheet.docx");
            assertThat(getPageContents(pdDocument, 1)).contains("Template: CV-PCS-CLM-ENG-Defence-Form.docx");
        }

        try (PDDocument pdDocument = findPackPdf("Delia Defendant", letters)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(2);
            // The fake document renderer writes the template name into the PDF page
            assertThat(getPageContents(pdDocument, 0)).contains("Template: CV-PCS-LET-ENG-Coversheet.docx");
            assertThat(getPageContents(pdDocument, 1)).contains("Template: CV-PCS-CLM-ENG-Defence-Form.docx");
        }

    }

    @Test
    @Order(3)
    void verifyResponsePackWithCounterClaim() throws IOException {
        Instant testStart = Instant.now();

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

        caseDocumentService.waitForDocumentsOfType(caseReference, DocumentType.DEFENDANT_RESPONSE, testStart, 1);
        caseDocumentService.waitForDocumentsOfType(caseReference, DocumentType.COUNTERCLAIM, testStart, 1);

        testingSupportClient.executeFutureDbSchedulerTasks(BULK_PRINT_TASK_NAME, Duration.ofDays(2));

        List<Letter> letters = mockSendLetterServer.getSentLetters(caseReference, DEFENCE_PACK, testStart, 2);
        assertThat(letters)
            .map(PostAndEmailNotificationsTest::getRecipientName)
            .containsExactlyInAnyOrder("Danny Defendant", "Delia Defendant");

        try (PDDocument pdDocument = findPackPdf("Danny Defendant", letters)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(3);
            // The fake document renderer writes the template name into the PDF page
            assertThat(getPageContents(pdDocument, 0)).contains("Template: CV-PCS-LET-ENG-Coversheet.docx");
            assertThat(getPageContents(pdDocument, 1)).contains("Template: CV-PCS-CLM-ENG-Defence-Form.docx");
            assertThat(getPageContents(pdDocument, 2)).contains("Template: CV-PCS-CLM-ENG-Counterclaim-Form.docx");
        }

        try (PDDocument pdDocument = findPackPdf("Delia Defendant", letters)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(3);
            // The fake document renderer writes the template name into the PDF page
            assertThat(getPageContents(pdDocument, 0)).contains("Template: CV-PCS-LET-ENG-Coversheet.docx");
            assertThat(getPageContents(pdDocument, 1)).contains("Template: CV-PCS-CLM-ENG-Defence-Form.docx");
            assertThat(getPageContents(pdDocument, 2)).contains("Template: CV-PCS-CLM-ENG-Counterclaim-Form.docx");
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

    private static PDDocument findPackPdf(String recipientName, List<Letter> letters) throws IOException {
        Letter packLetter = findLetterSentTo(recipientName, letters);
        return Loader.loadPDF(Base64.decode(packLetter.documents.getFirst().content));
    }

    private static String getPageContents(PDDocument pdDocument, int pageIndex) throws IOException {
        try (InputStream inputStream = pdDocument.getPage(pageIndex).getContents()) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
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
