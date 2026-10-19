package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobWorker;
import uk.gov.hmcts.ccd.sdk.bundling.job.ManagedCaseBundles;
import uk.gov.hmcts.ccd.sdk.bundling.job.OutboxBundleJobService;
import uk.gov.hmcts.ccd.sdk.bundling.managed.CaseBundles;
import uk.gov.hmcts.ccd.sdk.bundling.managed.CaseBundleView;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.BundleTab;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Real PCS persistence and projection, real SDK rendering, and the SDK's existing CDAM fixture. */
@PcsCcdEventTest
@TestPropertySource(properties = {
    "ccd.bundling.managed.enabled=true", "ccd.bundling.job.enabled=true",
    "ccd.bundling.job.worker.poll-delay=1h"
})
class CaseBundleIT extends AbstractPostgresContainerIT {
    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private PcsCaseRepository pcsCases;
    @Autowired
    private DocumentRepository documents;
    @Autowired
    private CaseBundles bundles;
    @Autowired
    private OutboxBundleJobService jobs;
    @Autowired
    private BundleJobWorker worker;
    @Autowired
    private TransactionTemplate transaction;

    @Test
    void documentHookQueuesOnlyCommittedCascadedWrites() throws Exception {
        long reference = cases.issue();
        UUID key = caseKey(reference);
        var first = document("First.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        var second = document("Second.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 2));
        transaction.executeWithoutResult(status -> {
            add(reference, first, second);
            status.setRollbackOnly();
        });
        assertThat(bundles.view(CaseBundleDefinition.NAME, key.toString()).lifecycle())
            .isEqualTo(CaseBundleView.Lifecycle.NONE);
        transaction.executeWithoutResult(status -> add(reference, first, second));
        assertThat(bundles.view(CaseBundleDefinition.NAME, key.toString()).lifecycle())
            .isEqualTo(CaseBundleView.Lifecycle.PREPARING);
    }

    @Test
    void projectsTheJudgesBundleWithoutAddingItToTheDocumentTable() throws Exception {
        long reference = cases.issue();
        UUID key = caseKey(reference);
        transaction.executeWithoutResult(status -> add(reference,
            document("Witness.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 10)),
            document("Inspection.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 5)),
            document("Claim.pdf", CaseFileCategory.STATEMENTS_OF_CASE, LocalDate.of(2026, 1, 2))));
        run(key);
        var first = bundles.view(CaseBundleDefinition.NAME, key.toString()).current().orElseThrow();
        PCSCase judge = view(reference, "judge");
        // Reading back loses the bundle's Document fields: Jackson applies the unwrapped prefix to them too.
        assertThat(judge.getBundleTab().getPages()).isEqualTo(first.report().pageCount());
        assertThat(judge.getAllDocuments()).extracting(document -> document.getValue().getUrl())
            .doesNotContain(first.document().getUrl());
        assertThat(view(reference, "hearing-centre-admin").getBundleTab()).isIn(null, new BundleTab());
        transaction.executeWithoutResult(status -> assertThat(pcsCases.findByCaseReference(reference).orElseThrow()
            .getDocuments()).hasSize(3));
    }

    @Test
    void amendmentsAndSoftDeletionInvalidateTheBundleThroughTheSameDocumentHook() throws Exception {
        long reference = cases.issue();
        UUID key = caseKey(reference);
        var source = document("Witness.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        transaction.executeWithoutResult(status -> add(reference, source));
        run(key);
        UUID sourceId = documents.findByDocumentId(source.getDocumentId()).orElseThrow().getId();
        transaction.executeWithoutResult(status -> documents.findById(sourceId).orElseThrow()
            .setFileName("Amended witness.pdf"));
        assertThat(bundles.view(CaseBundleDefinition.NAME, key.toString()).lifecycle())
            .isEqualTo(CaseBundleView.Lifecycle.UPDATING);
        run(key);
        transaction.executeWithoutResult(status -> documents.deleteById(sourceId));
        run(key);
        assertThat(bundles.view(CaseBundleDefinition.NAME, key.toString()).current().orElseThrow()
            .report().documents()).isEmpty();
    }

    @Test
    void issuingACounterclaimInvalidatesItsPreviouslyHiddenDocuments() throws Exception {
        long reference = cases.issue();
        UUID key = caseKey(reference);
        UUID claimId = transaction.execute(status -> {
            var pcsCase = pcsCases.findByCaseReference(reference).orElseThrow();
            var counterclaim = CounterClaimEntity.builder().party(pcsCase.getParties().iterator().next())
                .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED).build();
            pcsCase.addCounterClaim(counterclaim);
            var source = document("Counterclaim.pdf", CaseFileCategory.STATEMENTS_OF_CASE, LocalDate.of(2026, 1, 1));
            source.setCounterClaim(counterclaim);
            pcsCase.addDocuments(List.of(source));
            return pcsCases.saveAndFlush(pcsCase).getCounterClaims().getFirst().getId();
        });
        run(key);
        assertThat(bundles.view(CaseBundleDefinition.NAME, key.toString()).current().orElseThrow()
            .report().request().allDocuments()).isEmpty();
        transaction.executeWithoutResult(status -> pcsCases.findByCaseReference(reference).orElseThrow()
            .getCounterClaims().stream().filter(claim -> claim.getId().equals(claimId)).findFirst().orElseThrow()
            .setStatus(CounterClaimState.COUNTER_CLAIM_ISSUED));
        assertThat(bundles.view(CaseBundleDefinition.NAME, key.toString()).lifecycle())
            .isEqualTo(CaseBundleView.Lifecycle.UPDATING);
        run(key);
        assertThat(bundles.view(CaseBundleDefinition.NAME, key.toString()).current().orElseThrow()
            .report().request().allDocuments()).extracting(source -> source.title())
            .containsExactly("Counterclaim.pdf");
    }

    @Test
    void missingSourcesAppearInTheJudgesTab() throws Exception {
        long reference = cases.issue();
        UUID key = caseKey(reference);
        var missing = document("Deleted.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        Document stored = events.documents().stored().stream()
            .filter(document -> document.getUrl().equals(missing.getUrl())).findFirst().orElseThrow();
        events.documents().delete(stored);
        transaction.executeWithoutResult(status -> add(reference, missing));
        run(key);
        var missingDetails = view(reference, "judge").getBundleTab().getMissingDocuments();
        assertThat(missingDetails).singleElement().satisfies(missingDocument -> {
            assertThat(missingDocument.getValue().getDocumentName()).isEqualTo("Deleted.pdf");
            assertThat(missingDocument.getValue().getFolder()).isEqualTo("Evidence");
            assertThat(missingDocument.getValue().getPage()).isPositive();
        });
    }

    private PCSCase view(long reference, String role) {
        return events.view(reference, events.registerActor("Bundle", "Reader", "caseworker-pcs", role));
    }

    private void run(UUID key) {
        UUID id = jobs.findLatest(ManagedCaseBundles.key(CaseBundleDefinition.NAME, key.toString()))
            .orElseThrow().externalId();
        worker.poll();
        await().atMost(Duration.ofSeconds(30)).until(() -> jobs.find(id).orElseThrow().state().terminal());
        assertThat(jobs.find(id).orElseThrow().failure()).isEmpty();
    }

    private UUID caseKey(long reference) {
        return pcsCases.findByCaseReference(reference).orElseThrow().getId();
    }

    private void add(long reference, DocumentEntity... sources) {
        PcsCaseEntity pcsCase = pcsCases.findByCaseReference(reference).orElseThrow();
        pcsCase.addDocuments(List.of(sources));
        pcsCases.save(pcsCase);
    }

    @lombok.SneakyThrows
    private DocumentEntity document(String filename, CaseFileCategory category, LocalDate date) {
        Document stored = events.documents().store(filename, "application/pdf", pdf("Content of " + filename));
        UUID id = UUID.fromString(stored.getUrl().substring(stored.getUrl().lastIndexOf('/') + 1));
        return DocumentEntity.builder().documentId(id).url(stored.getUrl()).binaryUrl(stored.getBinaryUrl())
            .fileName(filename).contentType("application/pdf").categoryId(category.getId()).issueDate(date)
            .type(DocumentType.WITNESS_STATEMENT).build();
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

}
