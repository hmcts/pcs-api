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
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentDownloadServiceTest {

    private static final String DOCUMENT_ID = "12345678-1234-1234-1234-123456789abc";
    private static final String AUTHORIZATION = "Bearer token";
    private static final String S2S_TOKEN = "s2s-token";

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
    void shouldDownloadDocumentSuccessfully() {
        // Given
        Resource mockResource = mock(Resource.class);
        String fileName = "test-document.pdf";
        MediaType contentType = MediaType.APPLICATION_PDF;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.set("original-file-name", fileName);

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
        assertThat(response.fileName()).isEqualTo(fileName);
        assertThat(response.mimeType()).isEqualTo(contentType.toString());
    }

    @Test
    void shouldUseDocumentIdWhenFileNameHeaderMissing() {
        // Given
        Resource mockResource = mock(Resource.class);
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
    void shouldUseDefaultMimeTypeWhenContentTypeHeaderMissing() {
        // Given
        Resource mockResource = mock(Resource.class);
        HttpHeaders headers = new HttpHeaders();
        headers.set("original-file-name", "test.pdf");

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
    void shouldPropagateExceptionFromApiCall() {
        // Given
        RuntimeException expectedException = new RuntimeException("API failure");
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
