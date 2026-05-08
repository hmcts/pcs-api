package uk.gov.hmcts.reform.pcs.document.service;

import feign.FeignException;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentDownloadServiceTest {

    private static final String AUTH_TOKEN = "user access token";
    private static final String S2S_TOKEN = "s2s token";
    private static final UUID DOCUMENT_ID = UUID.randomUUID();

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ResponseEntity<Resource> response;

    private DocumentDownloadService underTest;

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        underTest = new DocumentDownloadService(caseDocumentClientApi, authTokenGenerator, documentRepository);
    }

    @Test
    void shouldGetDocumentBinary() {
        // Given
        String overriddenFilename = "overridden.pdf";
        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, S2S_TOKEN, DOCUMENT_ID))
            .thenReturn(response);

        stubPersistedDocumentEntity(overriddenFilename);
        stubDownloadResponseContentType(MediaType.APPLICATION_PDF);
        Resource resource = stubResponseBody();

        // When
        DownloadedDocumentResponse response = underTest.downloadDocument(AUTH_TOKEN, DOCUMENT_ID);

        // Then
        assertThat(response.file()).isEqualTo(resource);
        assertThat(response.fileName()).isEqualTo(overriddenFilename);
        assertThat(response.mimeType()).isEqualTo(MediaType.APPLICATION_PDF.toString());
    }

    @Test
    void shouldUseDefaultContentTypeWhenNotInHeaders() {
        // Given
        String overriddenFilename = "overridden.pdf";
        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, S2S_TOKEN, DOCUMENT_ID))
            .thenReturn(response);

        stubPersistedDocumentEntity(overriddenFilename);
        stubDownloadResponseContentType(null);

        // When
        DownloadedDocumentResponse response = underTest.downloadDocument(AUTH_TOKEN, DOCUMENT_ID);

        // Then
        assertThat(response.mimeType()).isEqualTo("application/octet-stream");
    }

    @Test
    void shouldFetchFilenameFromCdamWhenNotInDB() {
        // Given
        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, S2S_TOKEN, DOCUMENT_ID))
            .thenReturn(response);

        stubPersistedDocumentEntity(null);
        stubDownloadResponseContentType(MediaType.APPLICATION_PDF);

        String documentNameFromCdam = "cdam-name.pdf";
        Document document = Document.builder()
            .originalDocumentName(documentNameFromCdam)
            .build();

        when(caseDocumentClientApi.getMetadataForDocument(AUTH_TOKEN, S2S_TOKEN, DOCUMENT_ID)).thenReturn(document);

        // When
        DownloadedDocumentResponse response = underTest.downloadDocument(AUTH_TOKEN, DOCUMENT_ID);

        // Then
        assertThat(response.fileName()).isEqualTo(documentNameFromCdam);
    }

    @Test
    void shouldUseFallbackNameWhenOriginalNameNotInCdam() {
        // Given
        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, S2S_TOKEN, DOCUMENT_ID))
            .thenReturn(response);

        stubPersistedDocumentEntity(null);
        stubDownloadResponseContentType(MediaType.APPLICATION_PDF);

        Document document = Document.builder()
            .originalDocumentName(null)
            .build();

        when(caseDocumentClientApi.getMetadataForDocument(AUTH_TOKEN, S2S_TOKEN, DOCUMENT_ID)).thenReturn(document);

        // When
        DownloadedDocumentResponse response = underTest.downloadDocument(AUTH_TOKEN, DOCUMENT_ID);

        // Then
        assertThat(response.fileName()).isEqualTo("Unknown filename");
    }

    @Test
    void shouldUseFallbackNameWhenCdamRequestFails() {
        // Given
        when(caseDocumentClientApi.getDocumentBinary(AUTH_TOKEN, S2S_TOKEN, DOCUMENT_ID))
            .thenReturn(response);

        stubPersistedDocumentEntity(null);
        stubDownloadResponseContentType(MediaType.APPLICATION_PDF);

        FeignException feignException = mock(FeignException.class);
        when(caseDocumentClientApi.getMetadataForDocument(AUTH_TOKEN, S2S_TOKEN, DOCUMENT_ID))
            .thenThrow(feignException);

        // When
        DownloadedDocumentResponse response = underTest.downloadDocument(AUTH_TOKEN, DOCUMENT_ID);

        // Then
        assertThat(response.fileName()).isEqualTo("Unknown filename");
    }

    private void stubPersistedDocumentEntity(String overriddenFilename) {
        DocumentEntity documentEntity = mock(DocumentEntity.class);
        when(documentRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(documentEntity));
        when(documentEntity.getFileName()).thenReturn(overriddenFilename);
    }

    private void stubDownloadResponseContentType(MediaType mediaType) {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(response.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.getContentType()).thenReturn(mediaType);
    }

    private Resource stubResponseBody() {
        Resource resource = mock(Resource.class);
        when(response.getBody()).thenReturn(resource);
        return resource;
    }

}
