package uk.gov.hmcts.reform.pcs.controllers;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;
import uk.gov.hmcts.reform.pcs.document.service.DocumentDownloadService;
import uk.gov.hmcts.reform.pcs.exception.DocumentDownloadException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentDownloadControllerTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final UUID DOCUMENT_ID = UUID.randomUUID();

    @Mock
    private DocumentDownloadService documentDownloadService;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private DownloadedDocumentResponse downloadedDocumentResponse;
    @Mock
    private InputStream inputStream;
    @Mock
    private ServletOutputStream outputStream;

    private DocumentDownloadController underTest;

    @BeforeEach
    void setUp() throws IOException {
        when(documentDownloadService.downloadDocument(anyString(), any(UUID.class)))
            .thenReturn(downloadedDocumentResponse);
        when(downloadedDocumentResponse.fileName()).thenReturn("test.pdf");

        Resource resource = mock(Resource.class);
        when(downloadedDocumentResponse.file()).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(httpServletResponse.getOutputStream()).thenReturn(outputStream);

        underTest = new DocumentDownloadController(documentDownloadService);
    }

    @Test
    void shouldCallDocumentDownloadWithAccessToken() {
        // When
        underTest.downloadDocumentById(ACCESS_TOKEN, DOCUMENT_ID, httpServletResponse);

        // Then
        verify(documentDownloadService).downloadDocument(ACCESS_TOKEN, DOCUMENT_ID);
    }

    @Test
    void shouldSetResponseContentType() {
        // Given
        String expectedContentType = "application/pdf";
        when(downloadedDocumentResponse.mimeType()).thenReturn(expectedContentType);

        // When
        underTest.downloadDocumentById(ACCESS_TOKEN, DOCUMENT_ID, httpServletResponse);

        // Then
        verify(httpServletResponse).setContentType(expectedContentType);
    }

    @Test
    void shouldSetContentDispositionWithSantisedAndEncodedFilenames() {
        // Given
        when(downloadedDocumentResponse.fileName()).thenReturn("test <foo>.pdf");

        // When
        underTest.downloadDocumentById(ACCESS_TOKEN, DOCUMENT_ID, httpServletResponse);

        // Then
        String expectedContentDisposition = "attachment; filename=\"test _foo_.pdf\";"
            + " filename*=UTF-8''test%20%3Cfoo%3E.pdf";

        verify(httpServletResponse).setHeader(HttpHeaders.CONTENT_DISPOSITION, expectedContentDisposition);
    }

    @Test
    void shouldTransferStreamAndThenFlush() throws IOException {
        // When
        underTest.downloadDocumentById(ACCESS_TOKEN, DOCUMENT_ID, httpServletResponse);

        // Then
        InOrder inOrder = inOrder(inputStream, outputStream);
        inOrder.verify(inputStream).transferTo(outputStream);
        inOrder.verify(outputStream).flush();
    }

    @Test
    void shouldThrowDocumentDownloadExceptionIfStreamingFails() throws IOException {
        // Given
        IOException streamingException = new IOException("test");
        when(inputStream.transferTo(any(OutputStream.class)))
            .thenThrow(streamingException);

        // When
        Throwable throwable = catchThrowable(() -> underTest.downloadDocumentById(
            ACCESS_TOKEN,
            DOCUMENT_ID,
            httpServletResponse
        ));

        // Then
        assertThat(throwable)
            .isInstanceOf(DocumentDownloadException.class)
            .hasCause(streamingException);
    }

}
