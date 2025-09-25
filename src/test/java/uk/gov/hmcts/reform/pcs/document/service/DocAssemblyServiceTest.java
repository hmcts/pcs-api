package uk.gov.hmcts.reform.pcs.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.docassembly.DocAssemblyClient;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyRequest;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyResponse;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.docassembly.exception.DocumentGenerationFailedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.pcs.document.model.JsonNodeFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocAssemblyService Tests")
class DocAssemblyServiceTest {

    @Mock
    private DocAssemblyClient docAssemblyClient;

    @Mock
    private IdamService idamService;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private DocAssemblyService docAssemblyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_USER_TOKEN = "Bearer system-user-token";
    private static final String SERVICE_AUTH_TOKEN = "service-auth-token";
    private static final String EXPECTED_DOCUMENT_URL = "http://dm-store/documents/123";
    private static final String TEMPLATE_ID = "CV-SPC-CLM-ENG-01356.docx";
    private static final String OUTPUT_FILENAME = "generated-document.pdf";
    private static final String APPLICANT_NAME = "John Doe";
    private static final String CASE_NUMBER = "PCS-123456789";

    @BeforeEach
    void setUp() {
        docAssemblyService = new DocAssemblyService(docAssemblyClient, idamService, authTokenGenerator);
    }

    @Nested
    @DisplayName("FormPayload Method Tests")
    class FormPayloadMethodTests {

        @Test
        @DisplayName("Should successfully generate document with FormPayload")
        void shouldSuccessfullyGenerateDocumentWithFormPayload() {
            final FormPayload formPayload = createValidFormPayload();
            DocAssemblyResponse mockResponse = createMockResponse(EXPECTED_DOCUMENT_URL);

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            String result = docAssemblyService.generateDocument(
                formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME);

            assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
            verify(idamService).getSystemUserAuthorisation();
            verify(authTokenGenerator).generate();
            verify(docAssemblyClient).generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            );
        }

        @Test
        @DisplayName("Should create request with correct properties for FormPayload")
        void shouldCreateRequestWithCorrectPropertiesForFormPayload() {
            final FormPayload formPayload = createValidFormPayload();
            DocAssemblyResponse mockResponse = createMockResponse(EXPECTED_DOCUMENT_URL);

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            docAssemblyService.generateDocument(
                formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME);

            ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
            verify(docAssemblyClient).generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                requestCaptor.capture());

            DocAssemblyRequest capturedRequest = requestCaptor.getValue();
            // Template ID is base64 encoded by DocAssemblyRequest
            assertThat(capturedRequest.getTemplateId()).isNotNull();
            assertThat(capturedRequest.getOutputType()).isEqualTo(OutputType.PDF);
            assertThat(capturedRequest.getFormPayload()).isEqualTo(formPayload);
            assertThat(capturedRequest.getOutputFilename()).isEqualTo(OUTPUT_FILENAME);
            assertThat(capturedRequest.getCaseTypeId()).isNotNull();
            assertThat(capturedRequest.getCaseTypeId()).startsWith("PCS");
            assertThat(capturedRequest.getJurisdictionId()).isEqualTo("PCS");
            assertThat(capturedRequest.isSecureDocStoreEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should throw DocAssemblyException when FormPayload is null")
        void shouldThrowDocAssemblyExceptionWhenFormPayloadIsNull() {
            assertThatThrownBy(() ->
                docAssemblyService.generateDocument(
                    (FormPayload) null, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("Unexpected error occurred during document generation")
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("FormPayload cannot be null");

            verify(docAssemblyClient, never()).generateOrder(any(), any(), any());
            verify(idamService, never()).getSystemUserAuthorisation();
            verify(authTokenGenerator, never()).generate();
        }

        @Test
        @DisplayName("Should handle DocumentGenerationFailedException for FormPayload")
        void shouldHandleDocumentGenerationFailedExceptionForFormPayload() {
            final FormPayload formPayload = createValidFormPayload();
            DocumentGenerationFailedException docException =
                new DocumentGenerationFailedException(new RuntimeException("Document generation failed"));

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenThrow(docException);

            assertThatThrownBy(() ->
                docAssemblyService.generateDocument(formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("Document generation failed")
                .hasCause(docException);
        }
    }

    @Nested
    @DisplayName("JsonNode Method Tests")
    class JsonNodeMethodTests {

        @Test
        @DisplayName("Should successfully generate document with JsonNode")
        void shouldSuccessfullyGenerateDocumentWithJsonNode() {
            final JsonNode formPayload = createValidJsonNode();
            DocAssemblyResponse mockResponse = createMockResponse(EXPECTED_DOCUMENT_URL);

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            String result = docAssemblyService.generateDocument(
                formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME);

            assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
            verify(idamService).getSystemUserAuthorisation();
            verify(authTokenGenerator).generate();
            verify(docAssemblyClient).generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            );
        }

        @Test
        @DisplayName("Should create JsonNodeFormPayload wrapper correctly")
        void shouldCreateJsonNodeFormPayloadWrapperCorrectly() {
            final JsonNode formPayload = createValidJsonNode();
            DocAssemblyResponse mockResponse = createMockResponse(EXPECTED_DOCUMENT_URL);

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            docAssemblyService.generateDocument(
                formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME);

            ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
            verify(docAssemblyClient).generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                requestCaptor.capture());

            DocAssemblyRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getFormPayload()).isInstanceOf(JsonNodeFormPayload.class);
            
            JsonNodeFormPayload wrapper = (JsonNodeFormPayload) capturedRequest.getFormPayload();
            assertThat(wrapper.getData()).isEqualTo(formPayload);
        }

        @Test
        @DisplayName("Should throw DocAssemblyException when JsonNode is null")
        void shouldThrowDocAssemblyExceptionWhenJsonNodeIsNull() {
            assertThatThrownBy(() ->
                docAssemblyService.generateDocument(
                    (JsonNode) null, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("Unexpected error occurred during document generation")
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("FormPayload cannot be null");

            verify(docAssemblyClient, never()).generateOrder(any(), any(), any());
            verify(idamService, never()).getSystemUserAuthorisation();
            verify(authTokenGenerator, never()).generate();
        }

        @Test
        @DisplayName("Should handle DocumentGenerationFailedException for JsonNode")
        void shouldHandleDocumentGenerationFailedExceptionForJsonNode() {
            final JsonNode formPayload = createValidJsonNode();
            DocumentGenerationFailedException docException =
                new DocumentGenerationFailedException(new RuntimeException("Document generation failed"));

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenThrow(docException);

            assertThatThrownBy(() ->
                docAssemblyService.generateDocument(formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("Document generation failed")
                .hasCause(docException);
        }

        @Test
        @DisplayName("Should handle complex JsonNode structure")
        void shouldHandleComplexJsonNodeStructure() {
            final JsonNode complexPayload = createComplexJsonNode();
            DocAssemblyResponse mockResponse = createMockResponse(EXPECTED_DOCUMENT_URL);

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            String result = docAssemblyService.generateDocument(
                complexPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME);

            assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
        }
    }

    @Nested
    @DisplayName("Common Functionality Tests")
    class CommonFunctionalityTests {

        @ParameterizedTest
        @ValueSource(strings = {"PDF", "DOCX"})
        @DisplayName("Should handle different output types")
        void shouldHandleDifferentOutputTypes(String outputTypeStr) {
            final JsonNode formPayload = createValidJsonNode();
            DocAssemblyResponse mockResponse = createMockResponse(EXPECTED_DOCUMENT_URL);
            OutputType outputType = OutputType.valueOf(outputTypeStr);

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            String result = docAssemblyService.generateDocument(
                formPayload, TEMPLATE_ID, outputType, OUTPUT_FILENAME);

            assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);

            ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
            verify(docAssemblyClient).generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                requestCaptor.capture());
            assertThat(requestCaptor.getValue().getOutputType()).isEqualTo(outputType);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Should handle null, empty and blank output filenames")
        void shouldHandleInvalidOutputFilenames(String outputFilename) {
            final JsonNode formPayload = createValidJsonNode();
            DocAssemblyResponse mockResponse = createMockResponse(EXPECTED_DOCUMENT_URL);

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            String result = docAssemblyService.generateDocument(
                formPayload, TEMPLATE_ID, OutputType.PDF, outputFilename);

            assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);

            ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
            verify(docAssemblyClient).generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                requestCaptor.capture());
            assertThat(requestCaptor.getValue().getOutputFilename()).isEqualTo(outputFilename);
        }

        @Test
        @DisplayName("Should handle empty template ID")
        void shouldHandleEmptyTemplateId() {
            final JsonNode formPayload = createValidJsonNode();
            DocAssemblyResponse mockResponse = createMockResponse(EXPECTED_DOCUMENT_URL);
            String emptyTemplateId = "";

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            String result = docAssemblyService.generateDocument(
                formPayload, emptyTemplateId, OutputType.PDF, OUTPUT_FILENAME);

            assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
            verify(docAssemblyClient).generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            );
        }

        @Test
        @DisplayName("Should handle valid template ID")
        void shouldHandleValidTemplateId() {
            final JsonNode formPayload = createValidJsonNode();
            DocAssemblyResponse mockResponse = createMockResponse(EXPECTED_DOCUMENT_URL);
            String validTemplateId = "valid-template.docx";

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            String result = docAssemblyService.generateDocument(
                formPayload, validTemplateId, OutputType.PDF, OUTPUT_FILENAME);

            assertThat(result).isEqualTo(EXPECTED_DOCUMENT_URL);
            verify(docAssemblyClient).generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            );
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should throw DocAssemblyException when rendition output location is null")
        void shouldThrowDocAssemblyExceptionWhenRenditionOutputLocationIsNull() {
            final JsonNode formPayload = createValidJsonNode();
            DocAssemblyResponse mockResponse = createMockResponse(null);

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            assertThatThrownBy(() ->
                docAssemblyService.generateDocument(formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("No document URL returned from Doc Assembly service");
        }

        @Test
        @DisplayName("Should throw DocAssemblyException when rendition output location is empty")
        void shouldThrowDocAssemblyExceptionWhenRenditionOutputLocationIsEmpty() {
            final JsonNode formPayload = createValidJsonNode();
            DocAssemblyResponse mockResponse = createMockResponse("");

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            assertThatThrownBy(() ->
                docAssemblyService.generateDocument(formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("No document URL returned from Doc Assembly service");
        }

        @Test
        @DisplayName("Should throw DocAssemblyException when unexpected exception occurs")
        void shouldThrowDocAssemblyExceptionWhenUnexpectedExceptionOccurs() {
            final JsonNode formPayload = createValidJsonNode();
            RuntimeException unexpectedException = new RuntimeException("Unexpected network error");

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenThrow(unexpectedException);

            assertThatThrownBy(() ->
                docAssemblyService.generateDocument(formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("Unexpected error occurred during document generation")
                .hasCause(unexpectedException);
        }

        @Test
        @DisplayName("Should not rethrow DocAssemblyException when it's already a DocAssemblyException")
        void shouldNotRethrowDocAssemblyExceptionWhenAlreadyThrown() {
            final JsonNode formPayload = createValidJsonNode();
            DocAssemblyException originalException = new DocAssemblyException("Original error");

            when(idamService.getSystemUserAuthorisation()).thenThrow(originalException);

            assertThatThrownBy(() ->
                docAssemblyService.generateDocument(formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("Original error")
                .isSameAs(originalException);
        }

        @Test
        @DisplayName("Should handle exception gracefully and not throw unexpected exceptions")
        void shouldHandleExceptionGracefullyAndNotThrowUnexpectedExceptions() {
            final JsonNode formPayload = createValidJsonNode();
            RuntimeException unexpectedException = new RuntimeException("Network timeout");

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenThrow(unexpectedException);

            assertThatCode(() ->
                docAssemblyService.generateDocument(formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class);
        }

        @Test
        @DisplayName("Should handle IdamService exception")
        void shouldHandleIdamServiceException() {
            final JsonNode formPayload = createValidJsonNode();
            RuntimeException idamException = new RuntimeException("IDAM service unavailable");

            when(idamService.getSystemUserAuthorisation()).thenThrow(idamException);

            assertThatThrownBy(() ->
                docAssemblyService.generateDocument(formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("Unexpected error occurred during document generation")
                .hasCause(idamException);
        }

        @Test
        @DisplayName("Should handle AuthTokenGenerator exception")
        void shouldHandleAuthTokenGeneratorException() {
            final JsonNode formPayload = createValidJsonNode();
            RuntimeException authException = new RuntimeException("Auth token generation failed");

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenThrow(authException);

            assertThatThrownBy(() ->
                docAssemblyService.generateDocument(formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("Unexpected error occurred during document generation")
                .hasCause(authException);
        }
    }

    @Nested
    @DisplayName("Response Processing Tests")
    class ResponseProcessingTests {

        @Test
        @DisplayName("Should extract document URL correctly from response")
        void shouldExtractDocumentUrlCorrectlyFromResponse() {
            final JsonNode formPayload = createValidJsonNode();
            String expectedUrl = "http://different-dm-store/documents/456";
            DocAssemblyResponse mockResponse = createMockResponse(expectedUrl);

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            String result = docAssemblyService.generateDocument(
                formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME);

            assertThat(result).isEqualTo(expectedUrl);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "http://dm-store/documents/123",
            "https://secure-dm-store/docs/abc-def-ghi",
            "/local/path/document.pdf"
        })
        @DisplayName("Should handle various document URL formats")
        void shouldHandleVariousDocumentUrlFormats(String documentUrl) {
            final JsonNode formPayload = createValidJsonNode();
            DocAssemblyResponse mockResponse = createMockResponse(documentUrl);

            when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
            when(docAssemblyClient.generateOrder(
                eq(SYSTEM_USER_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                any(DocAssemblyRequest.class)
            ))
                .thenReturn(mockResponse);

            String result = docAssemblyService.generateDocument(
                formPayload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME);

            assertThat(result).isEqualTo(documentUrl);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create service with dependencies")
        void shouldCreateServiceWithDependencies() {
            DocAssemblyService service = new DocAssemblyService(docAssemblyClient, idamService, authTokenGenerator);

            assertThat(service).isNotNull();
        }
    }

    private FormPayload createValidFormPayload() {
        try {
            String json = String.format("{\"applicantName\":\"%s\",\"caseNumber\":\"%s\"}",
                                      APPLICANT_NAME, CASE_NUMBER);
            JsonNode jsonNode = objectMapper.readTree(json);
            return new JsonNodeFormPayload(jsonNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create FormPayload", e);
        }
    }

    private JsonNode createValidJsonNode() {
        try {
            String json = String.format("{\"applicantName\":\"%s\",\"caseNumber\":\"%s\"}",
                                      APPLICANT_NAME, CASE_NUMBER);
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JsonNode", e);
        }
    }

    private JsonNode createComplexJsonNode() {
        try {
            String json = """
                {
                    "applicantName": "John Doe",
                    "caseNumber": "PCS-123456789",
                    "address": {
                        "street": "123 Main St",
                        "city": "London",
                        "postcode": "SW1A 1AA"
                    },
                    "documents": [
                        {"name": "passport", "type": "identity"},
                        {"name": "utility_bill", "type": "address_proof"}
                    ],
                    "metadata": {
                        "submissionDate": "2024-01-15",
                        "priority": "high"
                    }
                }
                """;
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create complex JsonNode", e);
        }
    }

    private DocAssemblyResponse createMockResponse(String documentUrl) {
        DocAssemblyResponse mockResponse = mock(DocAssemblyResponse.class);
        when(mockResponse.getRenditionOutputLocation()).thenReturn(documentUrl);
        return mockResponse;
    }
}