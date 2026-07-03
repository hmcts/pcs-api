package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
}
