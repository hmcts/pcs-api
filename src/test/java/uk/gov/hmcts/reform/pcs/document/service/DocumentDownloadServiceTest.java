package uk.gov.hmcts.reform.pcs.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentDownloadServiceTest {

    private static final String DOCUMENT_ID = "12345678-1234-1234-1234-123456789abc";
    private static final String AUTHORIZATION = "Bearer token";
    private static final String S2S_TOKEN = "s2s-token";
    private static final String ORIGINAL_FILENAME = "test-document.pdf";

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private DocumentDownloadService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentDownloadService(caseDocumentClientApi, authTokenGenerator);
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
    }

    @Test
    void shouldDownloadDocumentSuccessfullyWithMetadata() {
        // Given
        final Resource mockResource = mock(Resource.class);
        MediaType contentType = MediaType.APPLICATION_PDF;

        // Mock metadata response with original filename
        Document mockDocument = mock(Document.class);
        mockDocument.originalDocumentName = ORIGINAL_FILENAME;
        when(caseDocumentClientApi.getMetadataForDocument(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenReturn(mockDocument);

        // Mock binary download response
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);

        ResponseEntity<Resource> apiResponse = ResponseEntity.ok()
            .headers(headers)
            .body(mockResource);

        when(caseDocumentClientApi.getDocumentBinary(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenReturn(apiResponse);

        // When
        DownloadedDocumentResponse response = underTest.downloadDocument(AUTHORIZATION, DOCUMENT_ID);

        // Then
        assertThat(response.file()).isEqualTo(mockResource);
        assertThat(response.fileName()).isEqualTo(ORIGINAL_FILENAME);
        assertThat(response.mimeType()).isEqualTo(contentType.toString());

        // Verify both API calls were made
        verify(caseDocumentClientApi).getMetadataForDocument(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        );
        verify(caseDocumentClientApi).getDocumentBinary(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        );
    }

    @Test
    void shouldUseDocumentIdWhenMetadataReturnsNullFilename() {
        // Given
        final Resource mockResource = mock(Resource.class);

        // Mock metadata with null filename
        Document mockDocument = mock(Document.class);
        mockDocument.originalDocumentName = null;
        when(caseDocumentClientApi.getMetadataForDocument(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenReturn(mockDocument);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        ResponseEntity<Resource> apiResponse = ResponseEntity.ok()
            .headers(headers)
            .body(mockResource);

        when(caseDocumentClientApi.getDocumentBinary(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenReturn(apiResponse);

        // When
        DownloadedDocumentResponse response = underTest.downloadDocument(AUTHORIZATION, DOCUMENT_ID);

        // Then
        assertThat(response.fileName()).isEqualTo(DOCUMENT_ID);
    }

    @Test
    void shouldUseDocumentIdWhenMetadataReturnsEmptyFilename() {
        // Given
        final Resource mockResource = mock(Resource.class);

        // Mock metadata with empty filename
        Document mockDocument = mock(Document.class);
        mockDocument.originalDocumentName = "";
        when(caseDocumentClientApi.getMetadataForDocument(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenReturn(mockDocument);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        ResponseEntity<Resource> apiResponse = ResponseEntity.ok()
            .headers(headers)
            .body(mockResource);

        when(caseDocumentClientApi.getDocumentBinary(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenReturn(apiResponse);

        // When
        DownloadedDocumentResponse response = underTest.downloadDocument(AUTHORIZATION, DOCUMENT_ID);

        // Then
        assertThat(response.fileName()).isEqualTo(DOCUMENT_ID);
    }

    @Test
    void shouldUseDocumentIdWhenMetadataCallFails() {
        // Given
        Resource mockResource = mock(Resource.class);

        // Mock metadata call to throw exception
        when(caseDocumentClientApi.getMetadataForDocument(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenThrow(new RuntimeException("Metadata service unavailable"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        ResponseEntity<Resource> apiResponse = ResponseEntity.ok()
            .headers(headers)
            .body(mockResource);

        when(caseDocumentClientApi.getDocumentBinary(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenReturn(apiResponse);

        // When
        DownloadedDocumentResponse response = underTest.downloadDocument(AUTHORIZATION, DOCUMENT_ID);

        // Then
        assertThat(response.fileName()).isEqualTo(DOCUMENT_ID);
        // Verify binary download still proceeded despite metadata failure
        verify(caseDocumentClientApi).getDocumentBinary(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        );
    }

    @Test
    void shouldUseDefaultMimeTypeWhenContentTypeHeaderMissing() {
        // Given
        Resource mockResource = mock(Resource.class);

        Document mockDocument = mock(Document.class);
        mockDocument.originalDocumentName = ORIGINAL_FILENAME;
        when(caseDocumentClientApi.getMetadataForDocument(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenReturn(mockDocument);

        HttpHeaders headers = new HttpHeaders();
        // No content type set

        ResponseEntity<Resource> apiResponse = ResponseEntity.ok()
            .headers(headers)
            .body(mockResource);

        when(caseDocumentClientApi.getDocumentBinary(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenReturn(apiResponse);

        // When
        DownloadedDocumentResponse response = underTest.downloadDocument(AUTHORIZATION, DOCUMENT_ID);

        // Then
        assertThat(response.mimeType()).isEqualTo("application/octet-stream");
    }

    @Test
    void shouldThrowExceptionForInvalidDocumentId() {
        // Given
        String invalidDocumentId = "invalid-uuid";

        // When
        Throwable throwable = catchThrowable(() ->
                                                 underTest.downloadDocument(AUTHORIZATION, invalidDocumentId)
        );

        // Then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldPropagateExceptionFromBinaryDownloadCall() {
        // Given
        Document mockDocument = mock(Document.class);
        mockDocument.originalDocumentName = ORIGINAL_FILENAME;
        when(caseDocumentClientApi.getMetadataForDocument(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenReturn(mockDocument);

        RuntimeException expectedException = new RuntimeException("Binary download failed");
        when(caseDocumentClientApi.getDocumentBinary(
            eq(AUTHORIZATION),
            eq(S2S_TOKEN),
            any(UUID.class)
        )).thenThrow(expectedException);

        // When
        Throwable throwable = catchThrowable(() ->
                                                 underTest.downloadDocument(AUTHORIZATION, DOCUMENT_ID)
        );

        // Then
        assertThat(throwable).isEqualTo(expectedException);
    }
}
