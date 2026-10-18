package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.enforcement.batchprint.BatchPrintDocumentKind;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementTestDocumentSeederTest {

    private static final long CASE_REFERENCE = 1234123412341234L;
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final String SELF = "http://dm-store/documents/" + DOCUMENT_ID;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;

    private EnforcementTestDocumentSeeder underTest;

    @BeforeEach
    void setUp() {
        underTest = new EnforcementTestDocumentSeeder(pcsCaseService, currentEnforcementService,
                                                      caseDocumentClientApi, authTokenGenerator,
                                                      systemUpdateUserTokenProvider, new DocumentIdExtractor());
    }

    @Test
    void shouldUploadAttachAndFileThePlaceholderInTheEnforcementFolder() {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).documents(new ArrayList<>())
            .build();
        EnforcementOrderEntity enforcement = EnforcementOrderEntity.builder().build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        when(currentEnforcementService.findCurrentEnforcement(pcsCase)).thenReturn(Optional.of(enforcement));
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("system-token");
        when(authTokenGenerator.generate()).thenReturn("s2s-token");
        when(caseDocumentClientApi.uploadDocuments(eq("system-token"), eq("s2s-token"), any()))
            .thenReturn(new UploadResponse(List.of(uploaded())));

        Optional<String> fileName = underTest.seed(CASE_REFERENCE, BatchPrintDocumentKind.NOTICE_OF_EVICTION,
                                                   "Notice of eviction (rescheduled)");

        assertThat(fileName).contains("Notice of eviction rescheduled.pdf");
        ArgumentCaptor<DocumentUploadRequest> upload = ArgumentCaptor.forClass(DocumentUploadRequest.class);
        verify(caseDocumentClientApi).uploadDocuments(eq("system-token"), eq("s2s-token"), upload.capture());
        assertThat(upload.getValue().getFiles()).singleElement().satisfies(file -> {
            assertThat(file.getOriginalFilename()).isEqualTo("Notice of eviction rescheduled.pdf");
            assertThat(file.getContentType()).isEqualTo("application/pdf");
        });
        ArgumentCaptor<CaseDocumentsMetadata> attach = ArgumentCaptor.forClass(CaseDocumentsMetadata.class);
        verify(caseDocumentClientApi).patchDocument(eq("system-token"), eq("s2s-token"), attach.capture());
        assertThat(attach.getValue().getCaseId()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(attach.getValue().getDocumentHashTokens()).singleElement().satisfies(token -> {
            assertThat(token.getId()).isEqualTo(DOCUMENT_ID.toString());
            assertThat(token.getHashToken()).isEqualTo("hash");
        });

        assertThat(pcsCase.getDocuments()).singleElement().satisfies(document -> {
            assertThat(document.getType()).isEqualTo(DocumentType.NOTICE_OF_EVICTION);
            assertThat(document.getCategoryId()).isEqualTo("enforcement");
            assertThat(document.getDocumentId()).isEqualTo(DOCUMENT_ID);
            assertThat(document.getUrl()).isEqualTo(SELF);
            assertThat(document.getPcsCase()).isSameAs(pcsCase);
        });
        assertThat(enforcement.getDocuments()).extracting(DocumentEntity::getDocumentId).containsExactly(DOCUMENT_ID);
    }

    @Test
    void shouldFileNothingWhenTheCaseHasNoEnforcement() {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        when(currentEnforcementService.findCurrentEnforcement(pcsCase)).thenReturn(Optional.empty());

        assertThat(underTest.seed(CASE_REFERENCE, BatchPrintDocumentKind.WARRANT, "Warrant")).isEmpty();
        verify(caseDocumentClientApi, never()).uploadDocuments(any(), any(), any());
    }

    @Test
    void shouldRenderTheTitleAndCaseReference() throws IOException {
        byte[] pdf = EnforcementTestDocumentSeeder.render("Warrant", CASE_REFERENCE);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(document)).contains("Warrant", "Case " + CASE_REFERENCE);
        }
    }

    private static Document uploaded() {
        Document.Link self = new Document.Link();
        self.href = SELF;
        Document.Link binary = new Document.Link();
        binary.href = SELF + "/binary";
        Document.Links links = new Document.Links();
        links.self = self;
        links.binary = binary;
        return Document.builder().hashToken("hash").links(links).build();
    }
}
