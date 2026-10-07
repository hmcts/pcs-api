package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LetterDocumentFetcherTest {

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;

    @InjectMocks
    private LetterDocumentFetcher underTest;

    @Test
    @DisplayName("Fetches a document's bytes from CDAM")
    void shouldFetchDocumentBytesFromCdam() {
        UUID documentId = UUID.randomUUID();
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("user-token");
        when(caseDocumentClientApi.getDocumentBinary("user-token", "s2s", documentId))
            .thenReturn(ResponseEntity.ok(new ByteArrayResource("pdf-bytes".getBytes())));

        byte[] bytes = underTest.fetchBytes(documentId);

        assertThat(bytes).isEqualTo("pdf-bytes".getBytes());
        verify(caseDocumentClientApi).getDocumentBinary("user-token", "s2s", documentId);
    }

    @Test
    @DisplayName("Throws when the download is truncated below its Content-Length")
    void shouldThrowWhenStreamTruncated() {
        UUID documentId = UUID.randomUUID();
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("user-token");
        when(caseDocumentClientApi.getDocumentBinary("user-token", "s2s", documentId))
            .thenReturn(documentResponse("pdf-bytes".getBytes(), 20));

        assertThatThrownBy(() -> underTest.fetchBytes(documentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("truncated");
    }

    @Test
    @DisplayName("Throws when the download is empty")
    void shouldThrowWhenDownloadIsEmpty() {
        UUID documentId = UUID.randomUUID();
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("user-token");
        when(caseDocumentClientApi.getDocumentBinary("user-token", "s2s", documentId))
            .thenReturn(ResponseEntity.ok(new ByteArrayResource(new byte[0])));

        assertThatThrownBy(() -> underTest.fetchBytes(documentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no content");
    }

    @Test
    @DisplayName("Returns the bytes when they match the Content-Length")
    void shouldReturnBytesWhenContentLengthMatches() {
        UUID documentId = UUID.randomUUID();
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("user-token");
        when(caseDocumentClientApi.getDocumentBinary("user-token", "s2s", documentId))
            .thenReturn(documentResponse("pdf-bytes".getBytes(), "pdf-bytes".length()));

        assertThat(underTest.fetchBytes(documentId)).isEqualTo("pdf-bytes".getBytes());
    }

    private ResponseEntity<Resource> documentResponse(byte[] content, long contentLength) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(contentLength);
        return new ResponseEntity<>(new ByteArrayResource(content), headers, HttpStatus.OK);
    }
}
