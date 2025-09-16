/*
package uk.gov.hmcts.reform.pcs.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
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
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;

import feign.FeignException;
import org.mockito.Mockito;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Ignore("Temporarily ignored - needs update for new DocAssemblyClient API")
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

    @Mock
    private ObjectMapper objectMapper;

    private DocAssemblyService docAssemblyService;

    @BeforeEach
    void setUp() {
        docAssemblyService = new DocAssemblyService(docAssemblyApi, idamService, authTokenGenerator, objectMapper);
        
        // Setup default token generation with lenient stubbing
        lenient().when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
        lenient().when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        
        // Mock JSON response parsing
        String jsonResponse = "{\"renditionOutputLocation\":\"" + EXPECTED_DOCUMENT_URL + "\"}";
        lenient().when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenReturn(jsonResponse);
        
        // Mock ObjectMapper to return the expected URL
        try {
            JsonNode mockJsonNode = org.mockito.Mockito.mock(JsonNode.class);
            lenient().when(mockJsonNode.get("renditionOutputLocation")).thenReturn(mockJsonNode);
            lenient().when(mockJsonNode.asText()).thenReturn(EXPECTED_DOCUMENT_URL);
            lenient().when(objectMapper.readTree(jsonResponse)).thenReturn(mockJsonNode);
        } catch (Exception e) {
            // Ignore exception in test setup
        }
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

    @Test
    void shouldThrowDocAssemblyExceptionWhenJsonParsingFails() throws Exception {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        String invalidJsonResponse = "invalid json response";
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenReturn(invalidJsonResponse);
        
        RuntimeException jsonException = new RuntimeException("JSON parsing failed");
        when(objectMapper.readTree(invalidJsonResponse)).thenThrow(jsonException);

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("Failed to parse Doc Assembly response")
            .hasCause(jsonException);
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenRenditionOutputLocationIsNull() throws Exception {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        // Use a real JSON string with null field
        String jsonResponse = "{\"renditionOutputLocation\":null}";
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenReturn(jsonResponse);
        
        // Create service with real ObjectMapper for proper JSON parsing
        DocAssemblyService realObjectMapperService = new DocAssemblyService(
            docAssemblyApi, idamService, authTokenGenerator, new ObjectMapper()
        );
        
        // When / Then
        assertThatThrownBy(() -> realObjectMapperService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("No document URL returned from Doc Assembly service");
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenRenditionOutputLocationIsEmpty() throws Exception {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        String jsonResponse = "{\"renditionOutputLocation\":\"\"}";
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenReturn(jsonResponse);
        
        JsonNode mockJsonNode = org.mockito.Mockito.mock(JsonNode.class);
        JsonNode emptyNode = org.mockito.Mockito.mock(JsonNode.class);
        when(objectMapper.readTree(jsonResponse)).thenReturn(mockJsonNode);
        when(mockJsonNode.get("renditionOutputLocation")).thenReturn(emptyNode);
        when(emptyNode.asText()).thenReturn("");

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("No document URL returned from Doc Assembly service");
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenRenditionOutputLocationIsMissing() throws Exception {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        // Use a real JSON string with missing field
        String jsonResponse = "{}";
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenReturn(jsonResponse);
        
        // Create service with real ObjectMapper for proper JSON parsing
        DocAssemblyService realObjectMapperService = new DocAssemblyService(
            docAssemblyApi, idamService, authTokenGenerator, new ObjectMapper()
        );
        
        // When / Then
        assertThatThrownBy(() -> realObjectMapperService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("No document URL returned from Doc Assembly service");
    }

    @Test
    void shouldHandleValidRenditionOutputLocation() throws Exception {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        String jsonResponse = "{\"renditionOutputLocation\":\"" + EXPECTED_DOCUMENT_URL + "\"}";
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenReturn(jsonResponse);
        
        JsonNode mockJsonNode = org.mockito.Mockito.mock(JsonNode.class);
        JsonNode urlNode = org.mockito.Mockito.mock(JsonNode.class);
        when(objectMapper.readTree(jsonResponse)).thenReturn(mockJsonNode);
        when(mockJsonNode.get("renditionOutputLocation")).thenReturn(urlNode);
        when(urlNode.asText()).thenReturn(EXPECTED_DOCUMENT_URL);

        // When
        String result = docAssemblyService.generateDocument(request);

        // Then
        assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
    }


    @Test
    void shouldThrowDocAssemblyExceptionWhenFeignException400Occurs() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        FeignException.BadRequest feignException = Mockito.mock(FeignException.BadRequest.class);
        when(feignException.status()).thenReturn(400);
        when(feignException.getMessage()).thenReturn("Bad request error");
        
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenThrow(feignException);

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("Bad request to Doc Assembly service: Bad request error")
            .hasCause(feignException);
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenFeignException401Occurs() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        FeignException.Unauthorized feignException = Mockito.mock(FeignException.Unauthorized.class);
        when(feignException.status()).thenReturn(401);
        when(feignException.getMessage()).thenReturn("Unauthorized");
        
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenThrow(feignException);

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("Authorization failed for Doc Assembly service: Unauthorized")
            .hasCause(feignException);
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenFeignException403Occurs() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        FeignException.Forbidden feignException = Mockito.mock(FeignException.Forbidden.class);
        when(feignException.status()).thenReturn(403);
        when(feignException.getMessage()).thenReturn("Forbidden");
        
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenThrow(feignException);

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("Authorization failed for Doc Assembly service: Forbidden")
            .hasCause(feignException);
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenFeignException404Occurs() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        FeignException.NotFound feignException = Mockito.mock(FeignException.NotFound.class);
        when(feignException.status()).thenReturn(404);
        when(feignException.getMessage()).thenReturn("Not found");
        
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenThrow(feignException);

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("Doc Assembly service endpoint not found: Not found")
            .hasCause(feignException);
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenFeignException500Occurs() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        FeignException.InternalServerError feignException = Mockito.mock(FeignException.InternalServerError.class);
        when(feignException.status()).thenReturn(500);
        when(feignException.getMessage()).thenReturn("Internal server error");
        
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenThrow(feignException);

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("Doc Assembly service is temporarily unavailable: Internal server error")
            .hasCause(feignException);
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenFeignException503Occurs() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        FeignException.ServiceUnavailable feignException = Mockito.mock(FeignException.ServiceUnavailable.class);
        when(feignException.status()).thenReturn(503);
        when(feignException.getMessage()).thenReturn("Service unavailable");
        
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenThrow(feignException);

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("Doc Assembly service is temporarily unavailable: Service unavailable")
            .hasCause(feignException);
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenFeignException502Occurs() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        FeignException feignException = Mockito.mock(FeignException.class);
        when(feignException.status()).thenReturn(502);
        when(feignException.getMessage()).thenReturn("Bad gateway");
        
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenThrow(feignException);

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("Doc Assembly service is temporarily unavailable: Bad gateway")
            .hasCause(feignException);
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenFeignException504Occurs() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        FeignException feignException = Mockito.mock(FeignException.class);
        when(feignException.status()).thenReturn(504);
        when(feignException.getMessage()).thenReturn("Gateway timeout");
        
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenThrow(feignException);

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("Doc Assembly service is temporarily unavailable: Gateway timeout")
            .hasCause(feignException);
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenFeignExceptionWithUnknownStatusOccurs() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        FeignException feignException = Mockito.mock(FeignException.class);
        when(feignException.status()).thenReturn(418); // I'm a teapot
        when(feignException.getMessage()).thenReturn("I'm a teapot");
        
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenThrow(feignException);

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("Doc Assembly service request failed: I'm a teapot")
            .hasCause(feignException);
    }

    @Test
    void shouldThrowDocAssemblyExceptionWhenFeignExceptionWith5xxStatusOccurs() {
        // Given
        DocAssemblyRequest request = new DocAssemblyRequest();
        Map<String, Object> formPayload = new HashMap<>();
        formPayload.put("ccdCaseReference", "PCS-123456789");
        request.setFormPayload(formPayload);
        
        FeignException feignException = Mockito.mock(FeignException.class);
        when(feignException.status()).thenReturn(599);
        when(feignException.getMessage()).thenReturn("Unknown server error");
        
        when(docAssemblyApi.generateDocument(
            eq(SYSTEM_USER_TOKEN), 
            eq(SERVICE_AUTH_TOKEN), 
            any(DocAssemblyRequest.class)
        )).thenThrow(feignException);

        // When & Then
        assertThatThrownBy(() -> docAssemblyService.generateDocument(request))
            .isInstanceOf(DocAssemblyException.class)
            .hasMessage("Doc Assembly service error: Unknown server error")
            .hasCause(feignException);
    }
}
*/ 