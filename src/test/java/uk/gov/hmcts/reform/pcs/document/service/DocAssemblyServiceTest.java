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

    private static final String SYSTEM_USER_TOKEN = "Bearer system-user-token";
    private static final String SERVICE_AUTH_TOKEN = "service-auth-token";
    private static final String EXPECTED_DOCUMENT_URL = "http://dm-store/documents/123";
    private static final String TEMPLATE_ID = "CV-SPC-CLM-ENG-01356.docx";
    private static final String OUTPUT_FILENAME = "generated-document.pdf";

    @BeforeEach
    void setUp() {
        docAssemblyService = new DocAssemblyService(docAssemblyClient, idamService, authTokenGenerator);
    }

    @Nested
    @DisplayName("JsonNode Method Tests")
    class JsonNodeMethodTests {

        @Test
        @DisplayName("Should successfully generate document with JsonNode")
        void shouldSuccessfullyGenerateDocumentAndReturnUrl() {
            final FormPayload formPayload = mock(FormPayload.class);
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
        @DisplayName("Should throw IllegalArgumentException when form payload is null")
        void shouldThrowIllegalArgumentExceptionWhenFormPayloadIsNull() {
            assertThatThrownBy(() ->
                                   docAssemblyService.generateDocument(
                                       null, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("Unexpected error occurred during document generation")
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("formPayload cannot be null");

            verify(docAssemblyClient, never()).generateOrder(any(), any(), any());
            verify(idamService, never()).getSystemUserAuthorisation();
            verify(authTokenGenerator, never()).generate();
        }

        @Test
        @DisplayName("Should handle DocumentGenerationFailedException")
        void shouldHandleDocumentGenerationFailedException() {
            final FormPayload formPayload = mock(FormPayload.class);
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
                                   docAssemblyService.generateDocument(
                                       formPayload,
                                       TEMPLATE_ID,
                                       OutputType.PDF,
                                       OUTPUT_FILENAME
                                   ))
                .isInstanceOf(DocAssemblyException.class)
                .hasMessage("Document generation failed")
                .hasCause(docException);
        }

        @Nested
        @DisplayName("Common Functionality Tests")
        class CommonFunctionalityTests {

            @ParameterizedTest
            @ValueSource(strings = {"PDF", "DOCX"})
            @DisplayName("Should handle different output types")
            void shouldHandleDifferentOutputTypes(String outputTypeStr) {
                final FormPayload formPayload = mock(FormPayload.class);
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
                    requestCaptor.capture()
                );
                assertThat(requestCaptor.getValue().getOutputType()).isEqualTo(outputType);
            }

            @ParameterizedTest
            @NullAndEmptySource
            @ValueSource(strings = {"   "})
            @DisplayName("Should handle null, empty and blank output filenames")
            void shouldHandleInvalidOutputFilenames(String outputFilename) {
                final FormPayload formPayload = mock(FormPayload.class);
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
                    requestCaptor.capture()
                );
                assertThat(requestCaptor.getValue().getOutputFilename()).isEqualTo(outputFilename);
            }

            @Test
            @DisplayName("Should handle empty template ID")
            void shouldHandleEmptyTemplateId() {
                final FormPayload formPayload = mock(FormPayload.class);
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
                final FormPayload formPayload = mock(FormPayload.class);
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
                final FormPayload formPayload = mock(FormPayload.class);
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
                                       docAssemblyService.generateDocument(
                                           formPayload,
                                           TEMPLATE_ID,
                                           OutputType.PDF,
                                           OUTPUT_FILENAME
                                       ))
                    .isInstanceOf(DocAssemblyException.class)
                    .hasMessage("No document URL returned from Doc Assembly service");
            }

            @Test
            @DisplayName("Should throw DocAssemblyException when rendition output location is empty")
            void shouldThrowDocAssemblyExceptionWhenRenditionOutputLocationIsEmpty() {
                final FormPayload formPayload = mock(FormPayload.class);
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
                                       docAssemblyService.generateDocument(
                                           formPayload,
                                           TEMPLATE_ID,
                                           OutputType.PDF,
                                           OUTPUT_FILENAME
                                       ))
                    .isInstanceOf(DocAssemblyException.class)
                    .hasMessage("No document URL returned from Doc Assembly service");
            }

            @Test
            @DisplayName("Should throw DocAssemblyException when unexpected exception occurs")
            void shouldThrowDocAssemblyExceptionWhenUnexpectedExceptionOccurs() {
                final FormPayload formPayload = mock(FormPayload.class);
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
                                       docAssemblyService.generateDocument(
                                           formPayload,
                                           TEMPLATE_ID,
                                           OutputType.PDF,
                                           OUTPUT_FILENAME
                                       ))
                    .isInstanceOf(DocAssemblyException.class)
                    .hasMessage("Unexpected error occurred during document generation")
                    .hasCause(unexpectedException);
            }

            @Test
            @DisplayName("Should not rethrow DocAssemblyException when it's already a DocAssemblyException")
            void shouldNotRethrowDocAssemblyExceptionWhenAlreadyThrown() {
                final FormPayload formPayload = mock(FormPayload.class);
                DocAssemblyException originalException = new DocAssemblyException("Original error");

                when(idamService.getSystemUserAuthorisation()).thenThrow(originalException);

                assertThatThrownBy(() ->
                                       docAssemblyService.generateDocument(
                                           formPayload,
                                           TEMPLATE_ID,
                                           OutputType.PDF,
                                           OUTPUT_FILENAME
                                       ))
                    .isInstanceOf(DocAssemblyException.class)
                    .hasMessage("Original error")
                    .isSameAs(originalException);
            }

            @Test
            @DisplayName("Should handle exception gracefully and not throw unexpected exceptions")
            void shouldHandleExceptionGracefullyAndNotThrowUnexpectedExceptions() {
                final FormPayload formPayload = mock(FormPayload.class);
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
                                   docAssemblyService.generateDocument(
                                       formPayload,
                                       TEMPLATE_ID,
                                       OutputType.PDF,
                                       OUTPUT_FILENAME
                                   ))
                    .isInstanceOf(DocAssemblyException.class);
            }

            @Test
            @DisplayName("Should handle IdamService exception")
            void shouldHandleIdamServiceException() {
                final FormPayload formPayload = mock(FormPayload.class);
                RuntimeException idamException = new RuntimeException("IDAM service unavailable");

                when(idamService.getSystemUserAuthorisation()).thenThrow(idamException);

                assertThatThrownBy(() ->
                                       docAssemblyService.generateDocument(
                                           formPayload,
                                           TEMPLATE_ID,
                                           OutputType.PDF,
                                           OUTPUT_FILENAME
                                       ))
                    .isInstanceOf(DocAssemblyException.class)
                    .hasMessage("Unexpected error occurred during document generation")
                    .hasCause(idamException);
            }

            @Test
            @DisplayName("Should handle AuthTokenGenerator exception")
            void shouldHandleAuthTokenGeneratorException() {
                final FormPayload formPayload = mock(FormPayload.class);
                RuntimeException authException = new RuntimeException("Auth token generation failed");

                when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_USER_TOKEN);
                when(authTokenGenerator.generate()).thenThrow(authException);

                assertThatThrownBy(() ->
                                       docAssemblyService.generateDocument(
                                           formPayload,
                                           TEMPLATE_ID,
                                           OutputType.PDF,
                                           OUTPUT_FILENAME
                                       ))
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
                final FormPayload formPayload = mock(FormPayload.class);
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
                final FormPayload formPayload = mock(FormPayload.class);
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


        private DocAssemblyResponse createMockResponse(String documentUrl) {
            DocAssemblyResponse mockResponse = mock(DocAssemblyResponse.class);
            when(mockResponse.getRenditionOutputLocation()).thenReturn(documentUrl);
            return mockResponse;
        }
    }
}
