package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;
import uk.gov.hmcts.reform.pcs.document.service.DocumentDownloadService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentDownloadControllerTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final UUID DOCUMENT_ID = UUID.randomUUID();

    @Mock
    private DocumentDownloadService documentDownloadService;
    @Mock
    private DownloadedDocumentResponse downloadedDocumentResponse;

    private DocumentDownloadController underTest;

    @BeforeEach
    void setUp() {
        when(documentDownloadService.downloadDocument(anyString(), any(UUID.class)))
            .thenReturn(downloadedDocumentResponse);
        when(downloadedDocumentResponse.fileName()).thenReturn("test.pdf");

        underTest = new DocumentDownloadController(documentDownloadService);
    }

    @Test
    void shouldCallDocumentDownloadWithAccessToken() {
        // When
        underTest.downloadDocumentById(ACCESS_TOKEN, DOCUMENT_ID);

        // Then
        verify(documentDownloadService).downloadDocument(ACCESS_TOKEN, DOCUMENT_ID);
    }

    @Test
    void shouldSetResponseContentType() {
        // Given
        MediaType expectedContentType = MediaType.APPLICATION_PDF;
        when(downloadedDocumentResponse.mediaType()).thenReturn(expectedContentType);

        // When
        ResponseEntity<StreamingResponseBody> responseEntity
            = underTest.downloadDocumentById(ACCESS_TOKEN, DOCUMENT_ID);

        // Then
        assertThat(responseEntity.getHeaders())
            .contains(entry(HttpHeaders.CONTENT_TYPE, List.of(expectedContentType.toString())));
    }

    @Test
    void shouldSetContentDispositionWithSantisedAndEncodedFilenames() {
        // Given
        when(downloadedDocumentResponse.fileName()).thenReturn("test <foo>.pdf");

        // When
        ResponseEntity<StreamingResponseBody> responseEntity
            = underTest.downloadDocumentById(ACCESS_TOKEN, DOCUMENT_ID);

        // Then
        String expectedContentDisposition = "attachment; filename=\"test _foo_.pdf\";"
            + " filename*=UTF-8''test%20%3Cfoo%3E.pdf";

        assertThat(responseEntity.getHeaders())
            .contains(entry(HttpHeaders.CONTENT_DISPOSITION, List.of(expectedContentDisposition)));
    }

    @Test
    void shouldSetBodyAsStreamingEntity() throws IOException {
        // Given
        String expectedContent = "test";
        when(downloadedDocumentResponse.file()).thenReturn(new ByteArrayResource(expectedContent.getBytes(UTF_8)));

        // When
        ResponseEntity<StreamingResponseBody> responseEntity
            = underTest.downloadDocumentById(ACCESS_TOKEN, DOCUMENT_ID);

        // Then
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamingResponseBody streamingResponseBody = responseEntity.getBody();
        streamingResponseBody.writeTo(outputStream);

        assertThat(outputStream.toString(UTF_8)).isEqualTo(expectedContent);
    }

}
