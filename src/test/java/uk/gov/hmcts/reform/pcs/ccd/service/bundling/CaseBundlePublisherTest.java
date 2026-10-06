package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleArtifact;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleDocument;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleExecutionContext;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleRequest;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleResult;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleSection;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleWarning;
import uk.gov.hmcts.ccd.sdk.bundling.api.DocumentReference;
import uk.gov.hmcts.ccd.sdk.bundling.api.DocumentResult;
import uk.gov.hmcts.ccd.sdk.bundling.api.EmptySectionPolicy;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJob;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobContext;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobState;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.bundling.CaseBundleDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.bundling.CaseBundleFolder;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseBundleEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseBundleRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseBundlePublisherTest {

    private static final String AUTH = "Bearer system-user";
    private static final String S2S = "s2s-token";
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID CDAM_ID = UUID.randomUUID();
    private static final String SELF_URL = "http://cdam/cases/documents/" + CDAM_ID;
    private static final String FILE_NAME = CASE_REFERENCE + "-case-bundle.pdf";
    private static final Instant CLAIMED_AT = Instant.parse("2026-10-05T23:30:00Z");
    private static final byte[] PDF = "%PDF-1.7".getBytes(StandardCharsets.UTF_8);

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private CaseBundleRepository caseBundleRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private Query lockQuery;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(CASE_ID).caseReference(CASE_REFERENCE).build();
    private final DocumentEntity evidence = DocumentEntity.builder()
        .id(UUID.randomUUID())
        .url("http://cdam/cases/documents/evidence")
        .description("Photos of the property")
        .build();
    private final UUID unrecordedDocumentId = UUID.randomUUID();

    private CaseBundlePublisher underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseBundlePublisher(caseDocumentClientApi, authTokenGenerator, systemUpdateUserTokenProvider,
                                            documentRepository, caseBundleRepository, entityManager,
                                            coreCaseDataApi, transactionManager);
    }

    @Test
    void shouldUploadTheBundleToCdamAsTheSystemUser() throws IOException {
        givenANewBundleIsStored(Optional.empty());

        publish(job(Optional.of(CLAIMED_AT)));

        ArgumentCaptor<DocumentUploadRequest> upload = ArgumentCaptor.forClass(DocumentUploadRequest.class);
        verify(caseDocumentClientApi).uploadDocuments(eq(AUTH), eq(S2S), upload.capture());
        assertThat(upload.getValue().getClassification()).isEqualTo(CaseBundlePublisher.CLASSIFICATION);
        assertThat(upload.getValue().getCaseTypeId()).isEqualTo(CaseType.getCaseType());
        assertThat(upload.getValue().getJurisdictionId()).isEqualTo(CaseType.getJurisdictionId());
        assertThat(upload.getValue().getFiles().getFirst().getOriginalFilename()).isEqualTo(FILE_NAME);
        assertThat(upload.getValue().getFiles().getFirst().getBytes()).isEqualTo(PDF);
    }

    @Test
    void shouldAttachTheUploadToTheCaseThroughTheAttachEvent() throws IOException {
        givenANewBundleIsStored(Optional.empty());

        publish(job(Optional.of(CLAIMED_AT)));

        ArgumentCaptor<CaseDataContent> event = ArgumentCaptor.forClass(CaseDataContent.class);
        verify(coreCaseDataApi).createEvent(eq(AUTH), eq(S2S), eq(String.valueOf(CASE_REFERENCE)), event.capture());
        assertThat(event.getValue().getEvent().getId()).isEqualTo(EventId.attachCaseBundle.name());
        assertThat(event.getValue().getEventToken()).isEqualTo("event-token");
        assertThat(event.getValue().getData()).isEqualTo(Map.of("caseBundleAttachment", Map.of(
            "document_url", SELF_URL,
            "document_binary_url", SELF_URL + "/binary",
            "document_filename", FILE_NAME,
            "document_hash", "hash-token")));
    }

    @Test
    void shouldFileTheBundleAsACaseDocumentUnderTheCaseLock() throws IOException {
        givenANewBundleIsStored(Optional.empty());

        publish(job(Optional.of(CLAIMED_AT)));

        InOrder order = inOrder(lockQuery, documentRepository);
        order.verify(lockQuery).getSingleResult();
        order.verify(documentRepository).save(any());
        DocumentEntity stitched = stitchedDocument();
        assertThat(stitched.getPcsCase()).isSameAs(pcsCase);
        assertThat(stitched.getType()).isEqualTo(DocumentType.CASE_BUNDLE);
        assertThat(stitched.getCategoryId()).isEqualTo(CaseBundleScope.BUNDLES_CATEGORY_ID);
        assertThat(stitched.getDocumentId()).isEqualTo(CDAM_ID);
        assertThat(stitched.getUrl()).isEqualTo(SELF_URL);
        assertThat(stitched.getBinaryUrl()).isEqualTo(SELF_URL + "/binary");
        assertThat(stitched.getFileName()).isEqualTo(FILE_NAME);
        assertThat(stitched.getSize()).isEqualTo(PDF.length);
        // Selected just after midnight, UK time.
        assertThat(stitched.getIssueDate()).isEqualTo(LocalDate.of(2026, 10, 6));
    }

    @Test
    void shouldRecordHowTheBundleWasBuilt() throws IOException {
        givenANewBundleIsStored(Optional.empty());

        Object summary = publish(job(Optional.of(CLAIMED_AT)));

        CaseBundleEntity bundle = savedBundle();
        assertThat(bundle.getDocument()).isSameAs(stitchedDocument());
        assertThat(bundle.getJobId()).isEqualTo(JOB_ID);
        assertThat(bundle.getBundleTitle()).isEqualTo("Case bundle");
        assertThat(bundle.getStitchStatus()).isEqualTo("COMPLETED_WITH_WARNINGS");
        assertThat(bundle.getPageCount()).isEqualTo(12);
        assertThat(bundle.getSha256()).isEqualTo("bundle-sha");
        assertThat(bundle.getWarnings()).containsExactly("EMPTY_SECTION: Empty folder");
        assertThat(bundle.isHasTableOfContents()).isTrue();
        assertThat(bundle.isHasFolderCoversheets()).isTrue();
        assertThat(bundle.isHasCoversheets()).isFalse();
        assertThat(bundle.getPageNumberFormat()).isEqualTo("BOTTOM_CENTRE_N_OF_M");
        assertThat(bundle.getDocumentsSelectedAt()).isEqualTo(CLAIMED_AT);
        assertThat(bundle.isLatest()).isTrue();
        assertThat(summary).isEqualTo(Map.of(
            "caseBundleId", bundle.getId().toString(),
            "documentId", bundle.getDocument().getId().toString(),
            "pageCount", 12,
            "latest", true));
    }

    @Test
    void shouldRecordWhereEachDocumentLandedInItsFolder() throws IOException {
        givenANewBundleIsStored(Optional.empty());

        publish(job(Optional.of(CLAIMED_AT)));

        assertThat(savedBundle().getFolders()).containsExactly(
            new CaseBundleFolder("Evidence", CaseFileCategory.EVIDENCE.getId(), 0, List.of(
                new CaseBundleDocument(evidence.getId(), "photos.pdf", "Photos of the property",
                                       "http://cdam/cases/documents/evidence", LocalDate.of(2026, 1, 2), 0,
                                       3, 4, "photos-sha"))),
            new CaseBundleFolder("Not a category", null, 1, List.of(
                new CaseBundleDocument(unrecordedDocumentId, "unrecorded.pdf", null, null, null, 0,
                                       null, null, null))));
    }

    @Test
    void shouldReplaceTheLatestBundleWhenThisOneSelectedItsDocumentsLater() throws IOException {
        CaseBundleEntity previous = previousBundle(CLAIMED_AT.minusSeconds(60));
        givenANewBundleIsStored(Optional.of(previous));

        publish(job(Optional.of(CLAIMED_AT)));

        assertThat(previous.isLatest()).isFalse();
        InOrder order = inOrder(entityManager, caseBundleRepository);
        order.verify(entityManager).flush();
        order.verify(caseBundleRepository).save(any());
        assertThat(savedBundle().isLatest()).isTrue();
    }

    @Test
    void shouldNotDisplaceABundleThatSelectedItsDocumentsLater() throws IOException {
        CaseBundleEntity newer = previousBundle(CLAIMED_AT.plusSeconds(60));
        givenANewBundleIsStored(Optional.of(newer));

        publish(job(Optional.of(CLAIMED_AT)));

        assertThat(newer.isLatest()).isTrue();
        assertThat(savedBundle().isLatest()).isFalse();
        verify(entityManager, never()).flush();
    }

    @Test
    void shouldFallBackToTheSubmissionTimeForAJobWithNoClaimTime() throws IOException {
        givenANewBundleIsStored(Optional.empty());
        BundleJob neverClaimed = job(Optional.empty());

        publish(neverClaimed);

        assertThat(savedBundle().getDocumentsSelectedAt()).isEqualTo(neverClaimed.submittedAt());
    }

    @Test
    void shouldReturnTheStoredBundleWhenAJobRunsAgain() throws IOException {
        CaseBundleEntity stored = previousBundle(CLAIMED_AT);
        when(caseBundleRepository.findByJobId(JOB_ID)).thenReturn(Optional.of(stored));

        Object summary = publish(job(Optional.of(CLAIMED_AT)));

        assertThat(summary).isEqualTo(Map.of(
            "caseBundleId", stored.getId().toString(),
            "documentId", stored.getDocument().getId().toString(),
            "pageCount", stored.getPageCount(),
            "latest", true));
        verifyNoInteractions(caseDocumentClientApi, coreCaseDataApi, documentRepository);
        verify(caseBundleRepository, never()).save(any());
    }

    private void givenANewBundleIsStored(Optional<CaseBundleEntity> currentLatest) {
        lenient().when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(AUTH);
        lenient().when(authTokenGenerator.generate()).thenReturn(S2S);
        when(caseBundleRepository.findByJobId(JOB_ID)).thenReturn(Optional.empty());
        when(entityManager.find(PcsCaseEntity.class, CASE_ID)).thenReturn(pcsCase);
        when(caseDocumentClientApi.uploadDocuments(anyString(), anyString(), any()))
            .thenReturn(new UploadResponse(List.of(uploaded())));
        when(coreCaseDataApi.startEvent(AUTH, S2S, String.valueOf(CASE_REFERENCE), EventId.attachCaseBundle.name()))
            .thenReturn(StartEventResponse.builder().token("event-token").build());
        when(entityManager.createNativeQuery(anyString())).thenReturn(lockQuery);
        when(lockQuery.setParameter("caseId", CASE_ID)).thenReturn(lockQuery);
        when(entityManager.getReference(PcsCaseEntity.class, CASE_ID)).thenReturn(pcsCase);
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            DocumentEntity document = invocation.getArgument(0);
            document.setId(UUID.randomUUID());
            return document;
        });
        when(documentRepository.findAllById(any())).thenReturn(List.of(evidence));
        when(caseBundleRepository.findByPcsCase_IdAndLatestTrue(CASE_ID)).thenReturn(currentLatest);
        when(caseBundleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Object publish(BundleJob job) throws IOException {
        return underTest.onCompleted(job, jobContext(), request(), result());
    }

    private DocumentEntity stitchedDocument() {
        ArgumentCaptor<DocumentEntity> stitched = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(stitched.capture());
        return stitched.getValue();
    }

    private CaseBundleEntity savedBundle() {
        ArgumentCaptor<CaseBundleEntity> bundle = ArgumentCaptor.forClass(CaseBundleEntity.class);
        verify(caseBundleRepository).save(bundle.capture());
        return bundle.getValue();
    }

    private static CaseBundleEntity previousBundle(Instant documentsSelectedAt) {
        return CaseBundleEntity.builder()
            .id(UUID.randomUUID())
            .document(DocumentEntity.builder().id(UUID.randomUUID()).build())
            .pageCount(5)
            .documentsSelectedAt(documentsSelectedAt)
            .latest(true)
            .build();
    }

    private static Document uploaded() {
        Document.Link self = new Document.Link();
        self.href = SELF_URL;
        Document.Link binary = new Document.Link();
        binary.href = SELF_URL + "/binary";
        Document.Links links = new Document.Links();
        links.self = self;
        links.binary = binary;
        return Document.builder().links(links).hashToken("hash-token").build();
    }

    private static BundleJob job(Optional<Instant> claimedAt) {
        Instant submittedAt = Instant.parse("2026-10-05T23:00:00Z");
        return new BundleJob(JOB_ID, BundleJobState.IN_PROGRESS, 1, submittedAt, submittedAt,
                             Optional.of(CaseBundleScope.coalesceKey(CASE_ID)), 0, claimedAt,
                             Optional.empty(), Optional.empty());
    }

    private static BundleJobContext jobContext() {
        return new BundleJobContext(JOB_ID, Optional.empty(), CaseBundleScope.selectorParameters(CASE_ID),
                                    BundleExecutionContext.empty());
    }

    private BundleRequest request() {
        return BundleRequest.builder()
            .externalId(JOB_ID)
            .title("Case bundle")
            .fileName(FILE_NAME)
            .root(BundleSection.builder("Case bundle")
                      .section(BundleSection.builder("Evidence")
                                   .document(bundleDocument(evidence.getId(), "photos.pdf", LocalDate.of(2026, 1, 2)))
                                   .emptySectionPolicy(EmptySectionPolicy.INCLUDE_PLACEHOLDER)
                                   .build())
                      .section(BundleSection.builder("Not a category")
                                   .document(bundleDocument(unrecordedDocumentId, "unrecorded.pdf", null))
                                   .build())
                      .build())
            .build();
    }

    private static BundleDocument bundleDocument(UUID id, String title, LocalDate date) {
        return BundleDocument.builder()
            .id(id.toString())
            .title(title)
            .date(date)
            .reference(new DocumentReference(CaseBundleScope.CDAM_PROVIDER, UUID.randomUUID().toString()))
            .build();
    }

    private BundleResult result() {
        DocumentResult placed = new DocumentResult(evidence.getId().toString(),
                                                   new DocumentReference(CaseBundleScope.CDAM_PROVIDER, "cdam-id"),
                                                   "application/pdf", "photos-sha", 4, 3);
        return new BundleResult(new PdfArtifact(), List.of(BundleWarning.of("EMPTY_SECTION", "Empty folder")),
                                List.of(placed), Map.of(), () -> { });
    }

    private static final class PdfArtifact implements BundleArtifact {

        @Override
        public String fileName() {
            return FILE_NAME;
        }

        @Override
        public String mediaType() {
            return "application/pdf";
        }

        @Override
        public long size() {
            return PDF.length;
        }

        @Override
        public String sha256() {
            return "bundle-sha";
        }

        @Override
        public int pageCount() {
            return 12;
        }

        @Override
        public InputStream open() {
            return new ByteArrayInputStream(PDF);
        }
    }
}
