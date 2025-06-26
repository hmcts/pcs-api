package uk.gov.hmcts.reform.pcs.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.testingsupport.model.DocAssemblyRequest;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocAssemblyServiceTest {

    private static final String SYSTEM_USER_TOKEN = "system-user-token";
    private static final String SERVICE_AUTH_TOKEN = "service-auth-token";
    private static final String EXPECTED_DOCUMENT_URL = "http://dm-store/documents/123";

    @Mock
    private DocAssemblyApi docAssemblyApi;

    @Mock
    private IdamService idamService;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private DocAssemblyService docAssemblyService;

    @BeforeEach
    void setUp() {
        docAssemblyService = new DocAssemblyService(docAssemblyApi, idamService, authTokenGenerator);
        
        // Setup default token generation with lenient stubbing
        lenient().when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
        lenient().when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        // Default stub for all tests
        lenient().when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenReturn(EXPECTED_DOCUMENT_URL);
    }

    @Test
    void shouldGenerateDocumentWithDefaultValues() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        formPayload.put("caseName", "Test Case");
        request.setFormPayload(formPayload);

        // When
        String result = docAssemblyService.generateDocument(request);

        // Then
        assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
        
        ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
        verify(docAssemblyApi).generateDocument(eq(SYSTEM_USER_TOKEN), eq(SERVICE_AUTH_TOKEN), requestCaptor.capture());
        
        DocAssemblyRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTemplateId()).isEqualTo(
            Base64.getEncoder().encodeToString("CV-SPC-CLM-ENG-01356.docx".getBytes())
        );
        assertThat(capturedRequest.getOutputType()).isEqualTo("PDF");
        assertThat(capturedRequest.getFormPayload()).isEqualTo(formPayload);
    }

    @Test
    void shouldGenerateDocumentWithCustomTemplateAndOutputType() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        formPayload.put("caseName", "Test Case");
        request.setFormPayload(formPayload);
        request.setTemplateId("custom-template.docx");
        request.setOutputType("DOCX");

        // When
        String result = docAssemblyService.generateDocument(request);

        // Then
        assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
        
        ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
        verify(docAssemblyApi).generateDocument(eq(SYSTEM_USER_TOKEN), eq(SERVICE_AUTH_TOKEN), requestCaptor.capture());
        
        DocAssemblyRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTemplateId()).isEqualTo(
            Base64.getEncoder().encodeToString("custom-template.docx".getBytes())
        );
        assertThat(capturedRequest.getOutputType()).isEqualTo("DOCX");
        assertThat(capturedRequest.getFormPayload()).isEqualTo(formPayload);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldUseDefaultTemplateIdWhenTemplateIdIsNullOrEmptyOrWhitespace(String templateId) {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        request.setTemplateId(templateId);

        // When
        String result = docAssemblyService.generateDocument(request);

        // Then
        assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
        
        ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
        verify(docAssemblyApi).generateDocument(eq(SYSTEM_USER_TOKEN), eq(SERVICE_AUTH_TOKEN), requestCaptor.capture());
        
        DocAssemblyRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTemplateId()).isEqualTo(
            Base64.getEncoder().encodeToString("CV-SPC-CLM-ENG-01356.docx".getBytes())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldUseDefaultOutputTypeWhenOutputTypeIsNullOrEmptyOrWhitespace(String outputType) {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        request.setOutputType(outputType);

        // When
        String result = docAssemblyService.generateDocument(request);

        // Then
        assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
        
        ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
        verify(docAssemblyApi).generateDocument(eq(SYSTEM_USER_TOKEN), eq(SERVICE_AUTH_TOKEN), requestCaptor.capture());
        
        DocAssemblyRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getOutputType()).isEqualTo("PDF");
    }

    @Test
    void shouldPreserveExistingTemplateIdWhenProvided() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        request.setTemplateId("existing-template.docx");

        // When
        String result = docAssemblyService.generateDocument(request);

        // Then
        assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
        
        ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
        verify(docAssemblyApi).generateDocument(eq(SYSTEM_USER_TOKEN), eq(SERVICE_AUTH_TOKEN), requestCaptor.capture());
        
        DocAssemblyRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTemplateId()).isEqualTo(
            Base64.getEncoder().encodeToString("existing-template.docx".getBytes())
        );
    }

    @Test
    void shouldPreserveExistingOutputTypeWhenProvided() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        request.setOutputType("RTF");

        // When
        String result = docAssemblyService.generateDocument(request);

        // Then
        assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
        
        ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
        verify(docAssemblyApi).generateDocument(eq(SYSTEM_USER_TOKEN), eq(SERVICE_AUTH_TOKEN), requestCaptor.capture());
        
        DocAssemblyRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getOutputType()).isEqualTo("RTF");
    }

    @Test
    void shouldThrowExceptionWhenRequestIsNull() {
        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Request cannot be null");
    }

    @Test
    void shouldGenerateTokensCorrectly() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);

        // When
        docAssemblyService.generateDocument(request);

        // Then
        verify(idamService).getSystemUserAuthorisation();
        verify(authTokenGenerator).generate();
        verify(docAssemblyApi).generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        );
    }
} 