//package uk.gov.hmcts.reform.pcs.ccd.page.uploadSupportingDocuments;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
//import uk.gov.hmcts.ccd.sdk.api.Event;
//import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
//import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
//import uk.gov.hmcts.ccd.sdk.type.Document;
//import uk.gov.hmcts.ccd.sdk.type.ListValue;
//import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
//import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
//import uk.gov.hmcts.reform.pcs.ccd.domain.State;
//import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
//import uk.gov.hmcts.reform.pcs.ccd.page.uploadsupportingdocs.DocumentUploadCategoryA;
//
//import java.util.Arrays;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class DocumentUploadCategoryATest extends BasePageTest {
//
//    private Event<PCSCase, UserRole, State> event;
//
//    @BeforeEach
//    void setUp() {
//        event = buildPageInTestEvent(new DocumentUploadCategoryA());
//    }
//
//    @Test
//    void shouldPassValidationWhenNoDocumentsUploaded() {
//        CaseDetails<PCSCase, State> caseDetails = createCaseWithDocuments(null);
//
//        AboutToStartOrSubmitResponse<PCSCase, State> response = executeMidEvent(caseDetails);
//
//        assertThat(response.getErrors()).isEmpty();
//    }
//
//    @Test
//    void shouldPassValidationForValidDocumentTypes() {
//        List<ListValue<Document>> documents = Arrays.asList(
//            createDocument("document.pdf"),
//            createDocument("image.jpg"),
//            createDocument("photo.jpeg"),
//            createDocument("file.doc")
//        );
//        CaseDetails<PCSCase, State> caseDetails = createCaseWithDocuments(documents);
//
//        AboutToStartOrSubmitResponse<PCSCase, State> response = executeMidEvent(caseDetails);
//
//        assertThat(response.getErrors()).isEmpty();
//    }
//
//    @Test
//    void shouldFailValidationForInvalidDocumentTypes() {
//        List<ListValue<Document>> documents = Arrays.asList(
//            createDocument("document.txt"),
//            createDocument("image.png"),
//            createDocument("file.exe")
//        );
//        CaseDetails<PCSCase, State> caseDetails = createCaseWithDocuments(documents);
//
//        AboutToStartOrSubmitResponse<PCSCase, State> response = executeMidEvent(caseDetails);
//
//        assertThat(response.getErrors()).hasSize(3);
//        assertThat(response.getErrors()).allMatch(error ->
//                                                      error.contains("has an invalid file type. Only PDF, DOC, JPG, and JPEG files are allowed.")
//        );
//    }
//
//    @Test
//    void shouldFailValidationForDocumentsWithoutFilename() {
//        List<ListValue<Document>> documents = Arrays.asList(
//            createDocumentWithoutFilename(),
//            createDocument(""),
//            createDocument("   ")
//        );
//        CaseDetails<PCSCase, State> caseDetails = createCaseWithDocuments(documents);
//
//        AboutToStartOrSubmitResponse<PCSCase, State> response = executeMidEvent(caseDetails);
//
//        assertThat(response.getErrors()).hasSize(3);
//        assertThat(response.getErrors()).allMatch(error -> error.contains("has no filename"));
//    }
//
//    @Test
//    void shouldFailValidationForDocumentsWithoutExtension() {
//        List<ListValue<Document>> documents = List.of(
//            createDocument("document-without-extension")
//        );
//        CaseDetails<PCSCase, State> caseDetails = createCaseWithDocuments(documents);
//
//        AboutToStartOrSubmitResponse<PCSCase, State> response = executeMidEvent(caseDetails);
//
//        assertThat(response.getErrors()).hasSize(1);
//        assertThat(response.getErrors().getFirst()).contains("has no file extension");
//    }
//
//    @Test
//    void shouldValidateMixedDocumentsAndReturnOnlyErrors() {
//        List<ListValue<Document>> documents = Arrays.asList(
//            createDocument("valid.pdf"),
//            createDocument("invalid.txt"),
//            createDocument("another-valid.jpg"),
//            createDocumentWithoutFilename()
//        );
//        CaseDetails<PCSCase, State> caseDetails = createCaseWithDocuments(documents);
//
//        AboutToStartOrSubmitResponse<PCSCase, State> response = executeMidEvent(caseDetails);
//
//        assertThat(response.getErrors()).hasSize(2);
//        assertThat(response.getErrors()).anyMatch(error -> error.contains("invalid.txt"));
//        assertThat(response.getErrors()).anyMatch(error -> error.contains("has no filename"));
//    }
//
//    private AboutToStartOrSubmitResponse<PCSCase, State> executeMidEvent(CaseDetails<PCSCase, State> caseDetails) {
//        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "documentUploadCategoryA");
//        return midEvent.handle(caseDetails, null);
//    }
//
//    private CaseDetails<PCSCase, State> createCaseWithDocuments(List<ListValue<Document>> documents) {
//        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
//        PCSCase caseData = PCSCase.builder()
//            .supportingDocumentsCategoryA(documents)
//            .build();
//        caseDetails.setData(caseData);
//        return caseDetails;
//    }
//
//    private ListValue<Document> createDocument(String filename) {
//        Document document = new Document();
//        document.setFilename(filename);
//        document.setBinaryUrl("http://example.com/document");
//
//        ListValue<Document> listValue = new ListValue<>();
//        listValue.setValue(document);
//        return listValue;
//    }
//
//    private ListValue<Document> createDocumentWithoutFilename() {
//        Document document = new Document();
//        document.setFilename(null);
//        document.setBinaryUrl("http://example.com/document");
//
//        ListValue<Document> listValue = new ListValue<>();
//        listValue.setValue(document);
//        return listValue;
//    }
//}
