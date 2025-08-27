package uk.gov.hmcts.reform.pcs.ccd.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.testingsupport.model.DocAssemblyRequest;

@ExtendWith(MockitoExtension.class)
class DocumentGenerationServiceTest {

    @Mock
    private DocAssemblyService docAssemblyService;

    private DocumentGenerationService documentGenerationService;

    private static final String EXPECTED_DOCUMENT_URL = "http://dm-store/documents/123";
    private static final String TEMPLATE_ID = "CV-CMC-ENG-0010.docx";
    private static final String OUTPUT_TYPE = "PDF";

    @BeforeEach
    void setUp() {
        documentGenerationService = new DocumentGenerationService(docAssemblyService);
    }

    @Test
    void shouldGenerateDocumentSuccessfully() {
        // Given
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("applicantName", "John Doe");
        formPayload.put("caseNumber", "1234567890");

        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
            .thenReturn(EXPECTED_DOCUMENT_URL);

        // When
        Document result = documentGenerationService.generateDocument(TEMPLATE_ID, formPayload, OUTPUT_TYPE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFilename()).contains(TEMPLATE_ID.replace(".docx", ""));
        assertThat(result.getBinaryUrl()).isEqualTo(EXPECTED_DOCUMENT_URL + "/binary");
        assertThat(result.getUrl()).isEqualTo(EXPECTED_DOCUMENT_URL);
    }

    @Test
    void shouldGenerateDocumentWithDefaultOutputType() {
        // Given
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("applicantName", "John Doe");

        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
            .thenReturn(EXPECTED_DOCUMENT_URL);

        // When
        Document result = documentGenerationService.generateDocument(TEMPLATE_ID, formPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFilename()).endsWith(".pdf");
    }

    @Test
    void shouldCreateDocumentListValue() {
        // Given
        Document document = Document.builder()
            .filename("test.pdf")
            .binaryUrl("http://test.com/doc")
            .build();

        // When
        ListValue<Document> result = documentGenerationService.createDocumentListValue(document);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getValue()).isEqualTo(document);
    }

    @Test
    void shouldThrowExceptionWhenDocumentGenerationFails() {
        // Given
        Map<String, Object> formPayload = new HashMap<>();
        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
            .thenThrow(new RuntimeException("Service unavailable"));

        // When/Then
        assertThatThrownBy(() ->
            documentGenerationService.generateDocument(TEMPLATE_ID, formPayload, OUTPUT_TYPE))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Document generation failed");
    }
}
