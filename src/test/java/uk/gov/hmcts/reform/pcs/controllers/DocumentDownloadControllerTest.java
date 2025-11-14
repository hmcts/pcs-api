package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;
import uk.gov.hmcts.reform.pcs.document.service.DocumentDownloadService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentDownloadControllerTest {

    private static final String DOCUMENT_ID = "12345678-1234-1234-1234-123456789abc";
    private static final String AUTHORIZATION = "Bearer token";

    @Mock
    private DocumentDownloadService documentDownloadService;

    private DocumentDownloadController underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentDownloadController(documentDownloadService);
    }

    @Test
    void shouldDownloadDocumentSuccessfully() {
        // Given
        Resource mockResource = mock(Resource.class);
        String fileName = "test-document.pdf";
        String mimeType = "application/pdf";

        DownloadedDocumentResponse documentResponse = new DownloadedDocumentResponse(
            mockResource,
            fileName,
            mimeType
        );

        when(documentDownloadService.downloadDocument(AUTHORIZATION, DOCUMENT_ID))
            .thenReturn(documentResponse);

        // When
        ResponseEntity<Resource> response = underTest.downloadDocumentById(AUTHORIZATION, DOCUMENT_ID);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(mockResource);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.valueOf(mimeType));
        assertThat(response.getHeaders().getFirst("original-file-name")).isEqualTo(fileName);
    }

    @Test
    void shouldPropagateExceptionFromService() {
        // Given
        RuntimeException expectedException = new RuntimeException("Download failed");
        when(documentDownloadService.downloadDocument(AUTHORIZATION, DOCUMENT_ID))
            .thenThrow(expectedException);

        // When
        Throwable throwable = catchThrowable(() ->
                                                 underTest.downloadDocumentById(AUTHORIZATION, DOCUMENT_ID)
        );

        // Then
        assertThat(throwable).isEqualTo(expectedException);
    }
}
