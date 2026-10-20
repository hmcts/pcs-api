package uk.gov.hmcts.reform.pcs;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.bulkprint.MockSendLetterServer;
import uk.gov.hmcts.reform.pcs.bulkprint.model.Letter;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.client.TestingSupportClient;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.model.FeePaymentSummary;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.CaseDocumentService;
import uk.gov.hmcts.reform.pcs.service.CaseStateService;
import uk.gov.hmcts.reform.pcs.service.FeePaymentService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType.CLAIMANT_CLAIM_PACK;
import static uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType.DEFENDANT_CLAIM_PACK;
import static uk.gov.hmcts.reform.pcs.ccd.task.BulkPrintScheduledTask.BULK_PRINT_TASK_NAME;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class CreatePossessionClaimTest extends CftlibTest {

    @Autowired
    private CcdClient ccdClient;
    @Autowired
    private IdamClient idamClient;
    @Autowired
    private CaseStateService caseStateService;
    @Autowired
    private CaseDocumentService caseDocumentService;
    @Autowired
    private FeePaymentService feePaymentService;
    @Autowired
    private TestingSupportClient testingSupportClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockSendLetterServer mockSendLetterServer;

    private String solicitorToken;
    private Long caseReference;

    @BeforeAll
    void setup() {
        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
    }

    @Test
    @Order(1)
    void createPossessionClaim() {

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .addressLine2("Marylebone")
                                 .postTown("London")
                                 .county("Greater London")
                                 .postCode("NW1 6XE")
                                 .build()
            )
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        CaseDetails caseDetails = ccdClient.createCase(caseData, solicitorToken);

        caseReference = caseDetails.getId();
        assertThat(caseReference).isNotNull();

        CaseDetails retrievedCase = ccdClient.getCaseDetails(caseReference, solicitorToken);
        assertThat(retrievedCase.getState()).isEqualTo(State.AWAITING_SUBMISSION_TO_HMCTS.name());
    }

    @Test
    @Order(2)
    void resumePossessionClaim() {
        PCSCase caseData = PCSCase.builder()
            .caseManagementLocationNumber(20262)
            .regionId(1)
            .claimantInformation(ClaimantInformation.builder()
                                     .claimantName("CftLibTest Claimant")
                                     .isClaimantNameCorrect(VerticalYesNo.YES)
                                     .build())
            .claimantContactPreferences(ClaimantContactPreferences.builder()
                                            .overriddenClaimantContactAddress(createTestAddress())
                                            .build())
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .addAnotherDefendant(VerticalYesNo.YES)
            .defendant1(createDefendant("Danny", "Defendant"))
            .additionalDefendants(wrapListItems(List.of(createDefendant("Delia", "Jones"))))
            .noticeServed(YesOrNo.NO)
            .languageUsed(LanguageUsed.ENGLISH)
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .build();

        CaseResource caseResource
            = ccdClient.updateCase(resumePossessionClaim, caseReference, caseData, solicitorToken);

        assertThat(caseResource.getReference()).isNotBlank();

        CaseDetails retrievedCase = ccdClient.getCaseDetails(caseReference, solicitorToken);

        TypeReference<List<ListValue<Party>>> partyList = new TypeReference<>() {};

        List<ListValue<Party>> allDefendants = objectMapper.convertValue(
            retrievedCase.getData().get("allDefendants"),
            partyList
        );
        assertThat(allDefendants).hasSize(2);
        assertThat(allDefendants)
            .extracting(ListValue::getValue)
            .extracting(Party::getFirstName)
                .containsExactly("Danny", "Delia");

        assertThat(retrievedCase.getState()).isEqualTo(State.PENDING_CASE_ISSUED.name());
    }

    @Test
    @Order(3)
    void verifyClaimPacks() {
        List<FeePaymentSummary> feePaymentSummaries
            = feePaymentService.waitForFeePaymentRequests(caseReference, PaymentCallbackHandlerType.CLAIM);
        feePaymentService.simulatePayments(caseReference, feePaymentSummaries);
        caseStateService.waitForCaseState(caseReference, State.CASE_ISSUED, solicitorToken);

        caseDocumentService.waitForDocumentType(caseReference, DocumentType.CLAIM);
        testingSupportClient.executeFutureDbSchedulerTasks(BULK_PRINT_TASK_NAME, Duration.ofDays(2));

        List<Letter> claimantLetters = mockSendLetterServer.getSentLetters(caseReference, CLAIMANT_CLAIM_PACK, 1);
        Letter claimantLetter = claimantLetters.getFirst();
        assertThat(getRecipientName(claimantLetter)).isEqualTo("CftLibTest Claimant");

        List<Letter> defendantLetters = mockSendLetterServer.getSentLetters(caseReference, DEFENDANT_CLAIM_PACK, 2);
        assertThat(defendantLetters)
            .map(CreatePossessionClaimTest::getRecipientName)
            .containsExactlyInAnyOrder("Danny Defendant", "Delia Jones");
    }

    private DefendantDetails createDefendant(String firstname, String lastName) {
        return DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName(firstname)
            .lastName(lastName)
            .addressKnown(VerticalYesNo.YES)
            .correspondenceAddress(createTestAddress())
            .build();
    }

    private AddressUK createTestAddress() {
        return AddressUK.builder()
            .addressLine1("123 Baker Street")
            .addressLine2("Marylebone")
            .postTown("London")
            .county("Greater London")
            .postCode("NW1 6XE")
            .build();
    }

    private static String getRecipientName(Letter letter) {
        return letter.additionalData.getRecipients().getFirst();
    }

}
