//package uk.gov.hmcts.reform.pcs.ccd.event;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import uk.gov.hmcts.ccd.sdk.api.EventPayload;
//import uk.gov.hmcts.ccd.sdk.type.AddressUK;
//import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
//import uk.gov.hmcts.reform.pcs.ccd.domain.State;
//import uk.gov.hmcts.reform.pcs.ccd.service.DocumentGenerationService;
//import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
//
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@ExtendWith(MockitoExtension.class)
//class GenerateDocumentPocTest {
//
//    @Mock
//    private DocumentGenerationService documentGenerationService;
//
//    @Mock
//    private PcsCaseService pcsCaseService;
//
//    private GenerateDocumentPoc generateDocumentPoc;
//
//    private static final long CASE_REFERENCE = 1234567890L;
//
//    @BeforeEach
//    void setUp() {
//        generateDocumentPoc = new GenerateDocumentPoc(documentGenerationService, pcsCaseService);
//    }
//
//    @Test
//    void shouldStartEventWithEmptyClaimantName() {
//        // Given
//        EventPayload<PCSCase, State> eventPayload = createMockEventPayload();
//
//        // When
//        PCSCase result = generateDocumentPoc.start(eventPayload);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.getClaimantName()).isEqualTo("");
//    }
//
//    @Test
//    void shouldStartEventWithExistingClaimantName() {
//        // Given
//        EventPayload<PCSCase, State> eventPayload = createMockEventPayload();
//        PCSCase caseData = eventPayload.caseData();
//        caseData.setClaimantName("John Doe");
//
//        // When
//        PCSCase result = generateDocumentPoc.start(eventPayload);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.getClaimantName()).isEqualTo("John Doe");
//    }
//
//    @Test
//    void shouldExtractCaseDataForDocumentWithClaimantName() {
//        // Given
//        PCSCase pcsCase = PCSCase.builder()
//            .claimantName("John Doe")
//            .build();
//
//        // When
//        Map<String, Object> result = generateDocumentPoc.extractCaseDataForDocument(pcsCase, CASE_REFERENCE);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.get("applicantName")).isEqualTo("John Doe");
//        assertThat(result.get("caseNumber")).isEqualTo(String.valueOf(CASE_REFERENCE));
//        assertThat(result.get("documentType")).isEqualTo("Case Summary");
//    }
//
//    @Test
//    void shouldExtractCaseDataForDocumentWithOverriddenClaimantName() {
//        // Given
//        PCSCase pcsCase = PCSCase.builder()
//            .claimantName("")
//            .overriddenClaimantName("Jane Smith")
//            .build();
//
//        // When
//        Map<String, Object> result = generateDocumentPoc.extractCaseDataForDocument(pcsCase, CASE_REFERENCE);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.get("applicantName")).isEqualTo("Jane Smith");
//    }
//
//    @Test
//    void shouldExtractCaseDataForDocumentWithPropertyAddress() {
//        // Given
//        AddressUK address = AddressUK.builder()
//            .addressLine1("123 Test Street")
//            .postCode("TE1 1ST")
//            .build();
//
//        PCSCase pcsCase = PCSCase.builder()
//            .claimantName("John Doe")
//            .propertyAddress(address)
//            .build();
//
//        // When
//        Map<String, Object> result = generateDocumentPoc.extractCaseDataForDocument(pcsCase, CASE_REFERENCE);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.get("propertyAddress")).isEqualTo("123 Test Street");
//        assertThat(result.get("postCode")).isEqualTo("TE1 1ST");
//    }
//
//    @Test
//    void shouldExtractCaseDataForDocumentWithNoClaimantName() {
//        // Given
//        PCSCase pcsCase = PCSCase.builder()
//            .claimantName("")
//            .build();
//
//        // When
//        Map<String, Object> result = generateDocumentPoc.extractCaseDataForDocument(pcsCase, CASE_REFERENCE);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.get("applicantName")).isEqualTo("Not Specified");
//    }
//
//    private EventPayload<PCSCase, State> createMockEventPayload() {
//        PCSCase caseData = PCSCase.builder().build();
//        return new EventPayload<>(CASE_REFERENCE, caseData, null);
//    }
//}
