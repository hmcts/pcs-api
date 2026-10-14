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
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleErrorCode;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleExecutionContext;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleRequest;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleResult;
import uk.gov.hmcts.ccd.sdk.bundling.api.DocumentReference;
import uk.gov.hmcts.ccd.sdk.bundling.api.MissingDocument;
import uk.gov.hmcts.ccd.sdk.bundling.api.MissingDocumentReason;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJob;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobContext;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobState;
import uk.gov.hmcts.ccd.sdk.bundling.job.OutboxBundleJobService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
    private EntityManager entityManager;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;
    @Mock
    private OutboxBundleJobService bundleJobs;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private Query lockQuery;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(CASE_ID).caseReference(CASE_REFERENCE).build();
    private final BundleJob thisJob = job(Optional.of(CLAIMED_AT));

    private CaseBundlePublisher underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseBundlePublisher(caseDocumentClientApi, authTokenGenerator, systemUpdateUserTokenProvider,
                                            documentRepository, entityManager, coreCaseDataApi, bundleJobs,
                                            transactionManager);
        lenient().when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(AUTH);
        lenient().when(authTokenGenerator.generate()).thenReturn(S2S);
        when(entityManager.find(PcsCaseEntity.class, CASE_ID)).thenReturn(pcsCase);
    }

    @Test
    void shouldUploadTheBundleToCdamAsTheSystemUser() throws IOException {
        givenTheBundleIsStored(Optional.empty());

        publish(thisJob);

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
        givenTheBundleIsStored(Optional.empty());

        publish(thisJob);

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
    void shouldFileTheFirstBundleAsACaseDocumentUnderTheCaseLock() throws IOException {
        givenTheBundleIsStored(Optional.empty());

        Object summary = publish(thisJob);

        InOrder order = inOrder(lockQuery, documentRepository);
        order.verify(lockQuery).getSingleResult();
        order.verify(documentRepository).save(any());
        DocumentEntity stored = savedBundle();
        assertThat(stored.getPcsCase()).isSameAs(pcsCase);
        assertThat(stored.getType()).isEqualTo(DocumentType.CASE_BUNDLE);
        assertThat(stored.getCategoryId()).isEqualTo(CaseBundleScope.BUNDLES_CATEGORY_ID);
        assertThat(stored.getDocumentId()).isEqualTo(CDAM_ID);
        assertThat(stored.getUrl()).isEqualTo(SELF_URL);
        assertThat(stored.getBinaryUrl()).isEqualTo(SELF_URL + "/binary");
        assertThat(stored.getFileName()).isEqualTo(FILE_NAME);
        assertThat(stored.getSize()).isEqualTo(PDF.length);
        assertThat(stored.getBundleJobId()).isEqualTo(thisJob.externalId());
        assertThat(stored.getSubmittedDate()).isNotNull();
        // Selected just after midnight, UK time.
        assertThat(stored.getIssueDate()).isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(summary).isEqualTo(Map.of("stored", true, "documentId", stored.getId().toString(),
                                             "pageCount", 12, "missingDocuments", 1));
        verify(caseDocumentClientApi, never()).deleteDocument(anyString(), anyString(), any(), anyBoolean());
    }

    @Test
    void shouldReplaceTheStoredBundleInPlaceAndDeleteTheOldPdf() throws IOException {
        UUID oldCdamId = UUID.randomUUID();
        DocumentEntity current = storedBundleFrom(olderJob(), oldCdamId);
        givenTheBundleIsStored(Optional.of(current));

        publish(thisJob);

        assertThat(savedBundle()).isSameAs(current);
        assertThat(current.getDocumentId()).isEqualTo(CDAM_ID);
        assertThat(current.getBundleJobId()).isEqualTo(thisJob.externalId());
        verify(caseDocumentClientApi).deleteDocument(AUTH, S2S, oldCdamId, true);
    }

    @Test
    void shouldReplaceItsOwnBundleWhenTheJobRunsAgain() throws IOException {
        UUID earlierRenderCdamId = UUID.randomUUID();
        DocumentEntity current = storedBundleFrom(thisJob, earlierRenderCdamId);
        givenTheBundleIsStored(Optional.of(current));

        publish(thisJob);

        assertThat(current.getDocumentId()).isEqualTo(CDAM_ID);
        verify(caseDocumentClientApi).deleteDocument(AUTH, S2S, earlierRenderCdamId, true);
        verify(bundleJobs, never()).find(any());
    }

    @Test
    void shouldDiscardARenderOlderThanTheStoredBundleWithoutUploadingIt() throws IOException {
        DocumentEntity newer = storedBundleFrom(newerJob(), UUID.randomUUID());
        when(documentRepository.findByPcsCase_IdAndType(CASE_ID, DocumentType.CASE_BUNDLE))
            .thenReturn(Optional.of(newer));

        Object summary = publish(thisJob);

        assertThat(summary).isEqualTo(Map.of("stored", false));
        verifyNoInteractions(caseDocumentClientApi, coreCaseDataApi);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void shouldDeleteItsUploadWhenANewerBundleWasStoredWhileItWasAttached() throws IOException {
        DocumentEntity newer = storedBundleFrom(newerJob(), UUID.randomUUID());
        givenTheBundleIsStored(Optional.empty());
        when(documentRepository.findByPcsCase_IdAndType(CASE_ID, DocumentType.CASE_BUNDLE))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(newer));

        Object summary = publish(thisJob);

        assertThat(summary).isEqualTo(Map.of("stored", false));
        verify(documentRepository, never()).save(any());
        verify(caseDocumentClientApi).deleteDocument(AUTH, S2S, CDAM_ID, true);
    }

    @Test
    void shouldDateABundleNeverClaimedByWhenItWasSubmitted() throws IOException {
        givenTheBundleIsStored(Optional.empty());
        BundleJob neverClaimed = job(Optional.empty());

        publish(neverClaimed);

        assertThat(savedBundle().getIssueDate()).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    void shouldKeepTheNewBundleWhenTheOldPdfCannotBeDeleted() throws IOException {
        UUID oldCdamId = UUID.randomUUID();
        givenTheBundleIsStored(Optional.of(storedBundleFrom(olderJob(), oldCdamId)));
        doThrow(new IllegalStateException("CDAM down"))
            .when(caseDocumentClientApi).deleteDocument(AUTH, S2S, oldCdamId, true);

        Object summary = publish(thisJob);

        assertThat(summary).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
            .containsEntry("stored", true);
    }

    private void givenTheBundleIsStored(Optional<DocumentEntity> current) {
        lenient().when(documentRepository.findByPcsCase_IdAndType(CASE_ID, DocumentType.CASE_BUNDLE))
            .thenReturn(current);
        when(caseDocumentClientApi.uploadDocuments(anyString(), anyString(), any()))
            .thenReturn(new UploadResponse(List.of(uploaded())));
        when(coreCaseDataApi.startEvent(AUTH, S2S, String.valueOf(CASE_REFERENCE), EventId.attachCaseBundle.name()))
            .thenReturn(StartEventResponse.builder().token("event-token").build());
        when(entityManager.createNativeQuery(anyString())).thenReturn(lockQuery);
        when(lockQuery.setParameter("caseId", CASE_ID)).thenReturn(lockQuery);
        lenient().when(entityManager.getReference(PcsCaseEntity.class, CASE_ID)).thenReturn(pcsCase);
        lenient().when(documentRepository.save(any())).thenAnswer(invocation -> {
            DocumentEntity document = invocation.getArgument(0);
            if (document.getId() == null) {
                document.setId(UUID.randomUUID());
            }
            return document;
        });
    }

    private Object publish(BundleJob job) throws IOException {
        return underTest.onCompleted(job, jobContext(job), mock(BundleRequest.class), result());
    }

    private DocumentEntity savedBundle() {
        ArgumentCaptor<DocumentEntity> saved = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(saved.capture());
        return saved.getValue();
    }

    private DocumentEntity storedBundleFrom(BundleJob job, UUID cdamId) {
        if (!job.externalId().equals(thisJob.externalId())) {
            lenient().when(bundleJobs.find(job.externalId())).thenReturn(Optional.of(job));
        }
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(pcsCase)
            .type(DocumentType.CASE_BUNDLE)
            .documentId(cdamId)
            .bundleJobId(job.externalId())
            .build();
    }

    private static BundleJob olderJob() {
        return job(Optional.of(CLAIMED_AT.minusSeconds(60)));
    }

    private static BundleJob newerJob() {
        return job(Optional.of(CLAIMED_AT.plusSeconds(60)));
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
        Instant submittedAt = Instant.parse("2026-10-05T22:00:00Z");
        return new BundleJob(UUID.randomUUID(), BundleJobState.IN_PROGRESS, 1, submittedAt, submittedAt,
                             Optional.of(CaseBundleScope.coalesceKey(CASE_ID)), 0, claimedAt,
                             Optional.empty(), Optional.empty());
    }

    private static BundleJobContext jobContext(BundleJob job) {
        return new BundleJobContext(job.externalId(), Optional.empty(), CaseBundleScope.selectorParameters(CASE_ID),
                                    BundleExecutionContext.empty());
    }

    private static BundleResult result() {
        MissingDocument missing = new MissingDocument("doc-1", new DocumentReference(CaseBundleScope.CDAM_PROVIDER,
                                                                                    "cdam-1"),
                                                      MissingDocumentReason.NOT_FOUND,
                                                      BundleErrorCode.DOCUMENT_NOT_FOUND, "Not found in CDAM", 3);
        return new BundleResult(new PdfArtifact(), List.of(), List.of(), List.of(missing), Map.of(), () -> { });
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
