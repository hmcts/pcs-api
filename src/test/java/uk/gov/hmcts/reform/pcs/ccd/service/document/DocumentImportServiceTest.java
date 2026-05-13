package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentImportServiceTest {

    private static final String SYSTEM_AUTH_TOKEN = "system auth token";
    private static final String S2S_AUTH_TOKEN = "s2s auth token";
    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;
    @Mock
    private IdamService idamService;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Captor
    private ArgumentCaptor<List<DocumentEntity>> documentEntityListCaptor;

    private DocumentImportService underTest;

    @BeforeEach
    void setUp() {
        when(idamService.getSystemUserAuthorisation()).thenReturn(SYSTEM_AUTH_TOKEN);
        when(authTokenGenerator.generate()).thenReturn(S2S_AUTH_TOKEN);

        underTest = new DocumentImportService(pcsCaseService, caseDocumentClientApi, idamService, authTokenGenerator);
    }

    @Test
    void shouldAddDocumentToCaseWithMetadata() {
        // Given
        UUID documentId = UUID.randomUUID();
        Document.Links links = createLinks("test url", "test binary url");
        Document documentMetadata = Document.builder()
            .originalDocumentName("original filename")
            .links(links)
            .build();

        when(caseDocumentClientApi.getMetadataForDocument(SYSTEM_AUTH_TOKEN, S2S_AUTH_TOKEN, documentId))
            .thenReturn(documentMetadata);

        String documentUrl = "https://some.host/some/path/%s".formatted(documentId.toString());

        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        underTest.addDocumentToCase(CASE_REFERENCE, documentUrl, CaseFileCategory.HEARING_DOCUMENTS);

        // Then
        verify(pcsCaseEntity).addDocuments(documentEntityListCaptor.capture());
        assertThat(documentEntityListCaptor.getValue()).hasSize(1);

        DocumentEntity documentEntity = documentEntityListCaptor.getValue().getFirst();
        assertThat(documentEntity.getFileName()).isEqualTo("original filename");
        assertThat(documentEntity.getUrl()).isEqualTo("test url");
        assertThat(documentEntity.getBinaryUrl()).isEqualTo("test binary url");
        assertThat(documentEntity.getCategoryId()).isEqualTo(CaseFileCategory.HEARING_DOCUMENTS.getId());
    }

    @SuppressWarnings("SameParameterValue")
    private Document.Links createLinks(String url, String binaryUrl) {
        Document.Links links = new Document.Links();
        links.self = createLink(url);
        links.binary = createLink(binaryUrl);
        return links;
    }

    private Document.Link createLink(String href) {
        Document.Link link = new Document.Link();
        link.href = href;
        return link;
    }
}
