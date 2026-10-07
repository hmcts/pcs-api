package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import feign.FeignException;
import feign.Request;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleDocument;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleSection;
import uk.gov.hmcts.ccd.sdk.bundling.api.MissingDocumentReason;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJob;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobReport;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobState;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobWorker;
import uk.gov.hmcts.ccd.sdk.bundling.job.OutboxBundleJobService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.BundleTab;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.view.BundleTabView;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "ccd.bundling.job.enabled=true",
    // The test drives the worker itself.
    "ccd.bundling.job.worker.poll-delay=1h"
})
@ActiveProfiles("integration")
@DisplayName("Always-current case bundle")
class CaseBundleIT extends AbstractPostgresContainerIT {

    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private BundleTabView bundleTabView;
    @Autowired
    private OutboxBundleJobService bundleJobs;
    @Autowired
    private BundleJobWorker worker;
    @Autowired
    private NamedParameterJdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private CaseDocumentClientApi caseDocumentClientApi;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean(name = "systemUpdateUserTokenProvider")
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @MockitoBean
    private CoreCaseDataApi coreCaseDataApi;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        // The worker claims any waiting job, so start each test with an empty outbox.
        jdbc.update("delete from bundling.bundle_job", Map.of());
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("Bearer system");
        when(coreCaseDataApi.startEvent(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(StartEventResponse.builder().token("event-token").build());
    }

    @Test
    @DisplayName("queues one job per case for any number of document changes, and none for a rollback")
    void coalescesDocumentChangesIntoOneJobPerCase() {
        UUID caseId = newCase();

        tx.executeWithoutResult(status -> addDocuments(caseId,
            document("witness.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 10)),
            document("claim.pdf", CaseFileCategory.STATEMENTS_OF_CASE, LocalDate.of(2026, 1, 2))));
        BundleJob queued = bundleJobs.findLatest(CaseBundleScope.coalesceKey(caseId)).orElseThrow();
        assertThat(queued.state()).isEqualTo(BundleJobState.QUEUED);

        tx.executeWithoutResult(status -> addDocuments(caseId,
            document("photo.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 11))));
        tx.executeWithoutResult(status -> {
            addDocuments(caseId, document("never.pdf", CaseFileCategory.EVIDENCE, LocalDate.now()));
            status.setRollbackOnly();
        });

        assertThat(jobCount(caseId)).isEqualTo(1);
        assertThat(bundleJobs.findLatest(CaseBundleScope.coalesceKey(caseId)).orElseThrow().externalId())
            .isEqualTo(queued.externalId());
    }

    @Test
    @DisplayName("queues a regeneration when a document is amended")
    void queuesARegenerationWhenADocumentIsAmended() {
        UUID caseId = newCase();
        DocumentEntity witness = document("witness.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 10));
        tx.executeWithoutResult(status -> addDocuments(caseId, witness));
        jdbc.update("delete from bundling.bundle_job where coalesce_key = :key",
                    Map.of("key", CaseBundleScope.coalesceKey(caseId)));

        tx.executeWithoutResult(status -> pcsCaseRepository.findById(caseId).orElseThrow().getDocuments()
            .getFirst().setFileName("Witness statement (amended).pdf"));

        assertThat(jobCount(caseId)).isEqualTo(1);
    }

    @Test
    @DisplayName("ignores documents that never go in the bundle")
    void ignoresDocumentsOutsideTheBundle() {
        final UUID caseId = newCase();

        DocumentEntity accessCode = document("access-code.pdf", CaseFileCategory.UNCATEGORISED_DOCUMENTS, null);
        accessCode.setType(DocumentType.DEFENDANT_ACCESS_CODE);
        DocumentEntity bundle = document("old-bundle.pdf", CaseFileCategory.UNCATEGORISED_DOCUMENTS, null);
        bundle.setType(DocumentType.CASE_BUNDLE);
        bundle.setCategoryId(CaseBundleScope.BUNDLES_CATEGORY_ID);
        tx.executeWithoutResult(status -> addDocuments(caseId, accessCode, bundle));

        assertThat(jobCount(caseId)).isZero();
    }

    @Test
    @DisplayName("renders the case into a bundle, stores it, and replaces it when a document arrives")
    void regeneratesTheBundleInPlaceWhenADocumentIsAdded() throws Exception {
        stubCdam();
        UUID caseId = newCase();
        tx.executeWithoutResult(status -> addDocuments(caseId,
            document("Witness statement.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 10)),
            document("Inspection report.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 5)),
            document("Claim form.pdf", CaseFileCategory.STATEMENTS_OF_CASE, LocalDate.of(2026, 1, 2))));

        BundleJob firstJob = runQueuedJob(caseId);

        DocumentEntity first = storedBundle(caseId);
        assertThat(first.getCategoryId()).isEqualTo(CaseBundleScope.BUNDLES_CATEGORY_ID);
        assertThat(first.getBundleJobId()).isEqualTo(firstJob.externalId());
        BundleJobReport firstReport = bundleJobs.findReport(firstJob.externalId()).orElseThrow();
        assertThat(titles(firstReport, "Evidence")).containsExactly("Inspection report.pdf", "Witness statement.pdf");
        assertThat(titles(firstReport, "Statements of case")).containsExactly("Claim form.pdf");
        assertThat(firstReport.request().root().sections()).hasSize(CaseFileCategory.values().length);
        assertThat(firstReport.documents()).hasSize(3)
            .allSatisfy(placed -> assertThat(placed.startPage()).isPositive());
        assertThat(firstReport.missingDocuments()).isEmpty();
        // Storing the bundle's own document did not queue another bundle.
        assertThat(waitingJob(caseId)).isFalse();

        ArgumentCaptor<CaseDataContent> attach = ArgumentCaptor.forClass(CaseDataContent.class);
        verify(coreCaseDataApi).createEvent(anyString(), anyString(), anyString(), attach.capture());
        assertThat(attach.getValue().getEvent().getId()).isEqualTo("attachCaseBundle");
        assertThat(attach.getValue().getData().toString()).contains("document_hash=hash-");
        assertThat(uploadedBundleText()).contains("Inspection report.pdf", "Witness statement.pdf", "Claim form.pdf",
                                                  "Content of Claim form.pdf");
        verify(caseDocumentClientApi, never()).deleteDocument(anyString(), anyString(), any(UUID.class),
                                                              any(Boolean.class));

        tx.executeWithoutResult(status -> addDocuments(caseId,
            document("Late evidence.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 2, 1))));
        BundleJob secondJob = runQueuedJob(caseId);

        DocumentEntity second = storedBundle(caseId);
        assertThat(second.getId()).as("replaced in place").isEqualTo(first.getId());
        assertThat(second.getDocumentId()).isNotEqualTo(first.getDocumentId());
        assertThat(second.getBundleJobId()).isEqualTo(secondJob.externalId());
        assertThat(titles(bundleJobs.findReport(secondJob.externalId()).orElseThrow(), "Evidence"))
            .containsExactly("Inspection report.pdf", "Witness statement.pdf", "Late evidence.pdf");
        verify(caseDocumentClientApi).deleteDocument("Bearer system", "s2s", first.getDocumentId(), true);
    }

    @Test
    @DisplayName("replaces a document that cannot be fetched with a placeholder, and lists it in the Bundle tab")
    void listsMissingDocumentsInTheBundleTab() throws Exception {
        stubCdam();
        UUID caseId = newCase();
        DocumentEntity deleted = document("Deleted.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 3));
        when(caseDocumentClientApi.getDocumentBinary(anyString(), anyString(), eq(deleted.getDocumentId())))
            .thenThrow(FeignException.errorStatus("getDocumentBinary", notFound()));
        tx.executeWithoutResult(status -> addDocuments(caseId,
            document("Claim form.pdf", CaseFileCategory.STATEMENTS_OF_CASE, LocalDate.of(2026, 1, 2)), deleted));

        BundleJob job = runQueuedJob(caseId);

        assertThat(bundleJobs.findReport(job.externalId()).orElseThrow().missingDocuments())
            .singleElement()
            .satisfies(missing -> assertThat(missing.reason()).isEqualTo(MissingDocumentReason.NOT_FOUND));
        assertThat(uploadedBundleText()).contains("Deleted.pdf (missing)", MissingDocumentReason.NOT_FOUND.message());

        BundleTab tab = tx.execute(status -> {
            PCSCase pcsCase = PCSCase.builder().build();
            bundleTabView.setCaseFields(pcsCase, pcsCaseRepository.findById(caseId).orElseThrow());
            return pcsCase.getBundleTab();
        });
        assertThat(tab.getStatus()).isEqualTo("The bundle is up to date, but some documents could not be "
                                                  + "included. Each has a page in the bundle explaining why.");
        assertThat(tab.getBundle().getCategoryId()).isEqualTo(CaseBundleScope.BUNDLES_CATEGORY_ID);
        assertThat(tab.getPages()).isPositive();
        assertThat(tab.getMissingDocuments()).extracting(listValue -> listValue.getValue())
            .singleElement().satisfies(missing -> {
                assertThat(missing.getDocumentName()).isEqualTo("Deleted.pdf");
                assertThat(missing.getFolder()).isEqualTo("Evidence");
                assertThat(missing.getReason()).isEqualTo(MissingDocumentReason.NOT_FOUND.message());
                assertThat(missing.getPage()).isPositive();
            });
    }

    private BundleJob runQueuedJob(UUID caseId) {
        BundleJob job = bundleJobs.findLatest(CaseBundleScope.coalesceKey(caseId)).orElseThrow();
        worker.poll();
        waitUntil(() -> bundleJobs.find(job.externalId()).orElseThrow().state().terminal());
        BundleJob finished = bundleJobs.find(job.externalId()).orElseThrow();
        assertThat(finished.failure()).as("job failure").isEmpty();
        assertThat(finished.state()).isIn(BundleJobState.COMPLETED, BundleJobState.COMPLETED_WITH_WARNINGS);
        return finished;
    }

    private UUID newCase() {
        return tx.execute(status -> {
            PcsCaseEntity pcsCase = new PcsCaseEntity();
            pcsCase.setCaseReference(ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L,
                                                                           9_999_999_999_999_999L));
            return pcsCaseRepository.saveAndFlush(pcsCase).getId();
        });
    }

    private void addDocuments(UUID caseId, DocumentEntity... documents) {
        PcsCaseEntity pcsCase = pcsCaseRepository.findById(caseId).orElseThrow();
        pcsCase.addDocuments(List.of(documents));
        pcsCaseRepository.save(pcsCase);
    }

    private static DocumentEntity document(String fileName, CaseFileCategory category, LocalDate issueDate) {
        UUID cdamId = UUID.randomUUID();
        return DocumentEntity.builder()
            .documentId(cdamId)
            .fileName(fileName)
            .categoryId(category.getId())
            .contentType("application/pdf")
            .url("http://cdam/cases/documents/" + cdamId)
            .binaryUrl("http://cdam/cases/documents/" + cdamId + "/binary")
            .issueDate(issueDate)
            .build();
    }

    private void stubCdam() throws IOException {
        when(caseDocumentClientApi.getDocumentBinary(anyString(), anyString(), any(UUID.class)))
            .thenAnswer(invocation -> {
                UUID cdamId = invocation.getArgument(2);
                String fileName = documentRepository.findByDocumentId(cdamId).orElseThrow().getFileName();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.add("OriginalFileName", fileName);
                Resource body = new ByteArrayResource(pdf("Content of " + fileName));
                return ResponseEntity.ok().headers(headers).body(body);
            });
        when(caseDocumentClientApi.uploadDocuments(anyString(), anyString(), any(DocumentUploadRequest.class)))
            .thenAnswer(invocation -> {
                UUID stored = UUID.randomUUID();
                Document document = Document.builder()
                    .links(new Document.Links())
                    .hashToken("hash-" + stored)
                    .build();
                document.links.self = new Document.Link();
                document.links.self.href = "http://cdam/cases/documents/" + stored;
                document.links.binary = new Document.Link();
                document.links.binary.href = document.links.self.href + "/binary";
                return new UploadResponse(List.of(document));
            });
    }

    private String uploadedBundleText() throws IOException {
        ArgumentCaptor<DocumentUploadRequest> upload = ArgumentCaptor.forClass(DocumentUploadRequest.class);
        verify(caseDocumentClientApi, atLeastOnce()).uploadDocuments(anyString(), anyString(), upload.capture());
        try (PDDocument bundle = Loader.loadPDF(upload.getValue().getFiles().getFirst().getBytes())) {
            return new PDFTextStripper().getText(bundle);
        }
    }

    private static byte[] pdf(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 700);
                content.showText(text);
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private DocumentEntity storedBundle(UUID caseId) {
        return documentRepository.findByPcsCase_IdAndType(caseId, DocumentType.CASE_BUNDLE).orElseThrow();
    }

    private static List<String> titles(BundleJobReport report, String folder) {
        return report.request().root().sections().stream()
            .filter(section -> section.title().equals(folder))
            .map(BundleSection::documents)
            .findFirst()
            .orElseThrow()
            .stream()
            .map(BundleDocument::title)
            .toList();
    }

    private static feign.Response notFound() {
        return feign.Response.builder()
            .status(404)
            .reason("Not Found")
            .request(Request.create(Request.HttpMethod.GET, "http://cdam", Map.of(), null, StandardCharsets.UTF_8,
                                    null))
            .headers(Map.of())
            .build();
    }

    private int jobCount(UUID caseId) {
        return jdbc.queryForObject("select count(*) from bundling.bundle_job where coalesce_key = :key",
                                   Map.of("key", CaseBundleScope.coalesceKey(caseId)), Integer.class);
    }

    private boolean waitingJob(UUID caseId) {
        return jdbc.queryForObject("select count(*) from bundling.bundle_job where coalesce_key = :key "
                                       + "and state = 'QUEUED'",
                                   Map.of("key", CaseBundleScope.coalesceKey(caseId)), Integer.class) > 0;
    }

    private static void waitUntil(BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Timed out waiting for the bundle job");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }
}
