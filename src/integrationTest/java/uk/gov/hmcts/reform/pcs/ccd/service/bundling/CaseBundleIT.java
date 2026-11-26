package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.integrations.TestData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.ccd.sdk.doctoolkit.spring.DocJobsAutoConfiguration;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.BundleTab;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.bundle.CaseBundleEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseBundleRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Real PCS events, persistence and projection, the SDK's doc toolkit rendering for real, and the SDK's CDAM
 * fixture. Every change to the case goes through an event; the bundle follows.
 */
@PcsCcdEventTest
@Import(CaseBundleIT.Flags.class)
@TestPropertySource(properties = {
    "ccd.doc-toolkit.jobs.enabled=true", "ccd.doc-toolkit.jobs.worker.enabled=true",
    "ccd.doc-toolkit.jobs.worker.poll-delay=1h"
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
    private CaseBundleRepository versions;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DocJobsAutoConfiguration.DocJobWorkerLifecycle worker;
    @Autowired
    private SystemEventExecutor systemEvents;
    @Autowired
    private TransactionTemplate transaction;
    @Autowired
    private TestData flags;

    @TestConfiguration
    static class Flags {
        @Bean
        TestData bundleFlags() {
            return TestData.dataSource();
        }

        @Bean(destroyMethod = "close")
        @Primary
        LDClient bundleFlagClient(TestData data) {
            return new LDClient("test", new LDConfig.Builder().dataSource(data).events(
                com.launchdarkly.sdk.server.Components.noEvents()).build());
        }
    }

    @BeforeEach
    void hideBundles() {
        flags.update(flags.flag(FeatureFlag.CASE_BUNDLE_VIEW.key()).clearTargets().variationForAll(false));
        flags.update(flags.flag(FeatureFlag.MAKE_ORDER.key()).variationForAll(true));
    }

    @Test
    void caseHasABundleFromItsFirstEvent() throws Exception {
        long reference = issued();

        render(reference);

        String text = text(current(reference));
        assertThat(text).contains("Case bundle").contains("Index").contains("Evidence")
            .contains("There are no documents in this section.").contains("3 of 11");
    }

    @Test
    void anEventThatFailsQueuesNothing() throws Exception {
        long reference = issued();
        render(reference);

        assertThatThrownBy(() -> event(reference, pcsCase -> {
            pcsCase.addDocuments(List.of(document("Rolled back.pdf", CaseFileCategory.EVIDENCE,
                LocalDate.of(2026, 1, 1))));
            throw new IllegalStateException("the event fails");
        })).hasMessageContaining("the event fails");
        render(reference);

        assertThat(versions(reference)).hasSize(1);
    }

    @Test
    void anEventThatLeavesTheBundleAsItIsQueuesNothing() throws Exception {
        long reference = issued();
        render(reference);
        long builtAt = caseRevision(reference);
        assertThat(latest(reference).getCaseRevision()).isEqualTo(builtAt);

        // A change the bundle does not read.
        event(reference, pcsCase -> pcsCase.setPreActionProtocolCompleted(
            !Boolean.TRUE.equals(pcsCase.getPreActionProtocolCompleted())));
        render(reference);

        // The bundle still shows the case as at the revision it was built at, which is still accurate.
        assertThat(caseRevision(reference)).isGreaterThan(builtAt);
        assertThat(versions(reference)).singleElement()
            .satisfies(bundle -> assertThat(bundle.getCaseRevision()).isEqualTo(builtAt));
    }

    @Test
    void projectsTheJudgesBundleWithoutAddingItToTheDocumentTable() throws Exception {
        long reference = issued();
        event(reference, pcsCase -> pcsCase.addDocuments(List.of(
            document("Witness.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 10)),
            document("Inspection.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 5)),
            document("Claim.pdf", CaseFileCategory.STATEMENTS_OF_CASE, LocalDate.of(2026, 1, 2)))));
        render(reference);
        CaseBundleEntity first = latest(reference);
        flags.update(flags.flag(FeatureFlag.CASE_BUNDLE_VIEW.key()).variationForAll(true));
        PCSCase judge = view(reference, "judge");
        assertThat(judge.getBundleTab().getStatus()).isEqualTo("This is the latest bundle for the case.");
        assertThat(judge.getBundleTab().getPages()).isEqualTo(first.getPageCount());
        assertThat(judge.getBundleTab().getBundle().getUrl()).isEqualTo(first.getUrl());
        assertThat(judge.getBundleTab().getBundle().getCategoryId()).isEqualTo(CaseBundleDefinition.FOLDER_ID);
        assertThat(judge.getAllDocuments()).extracting(document -> document.getValue().getUrl())
            .doesNotContain(first.getUrl());
        assertThat(view(reference, "hearing-centre-admin").getBundleTab()).isIn(null, new BundleTab());
        // The judge making an order links it; nobody else is given it.
        assertThat(orderBundle(reference, "judge")).isEqualTo(new OrderStart.CaseBundle(
            first.getDocumentId(), first.getFileName(), first.getCreatedAt()));
        assertThat(orderBundle(reference, "hearing-centre-admin")).isNull();
        transaction.executeWithoutResult(status -> assertThat(pcsCases.findByCaseReference(reference).orElseThrow()
            .getDocuments()).hasSize(3));

        // Chronological within their folder, each folder in display order.
        String text = text(first);
        assertThat(text.indexOf("Claim.pdf")).isLessThan(text.indexOf("Inspection.pdf"));
        assertThat(text.indexOf("Inspection.pdf")).isLessThan(text.indexOf("Witness.pdf"));
        assertThat(text).contains("Content of Witness.pdf").contains("Content of Claim.pdf");
    }

    @Test
    void amendmentsAndRemovalUpdateTheBundleAndEarlierVersionsAreKept() throws Exception {
        long reference = issued();
        event(reference, pcsCase -> pcsCase.addDocuments(List.of(
            document("Witness.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1)))));
        render(reference);
        UUID sourceId = transaction.execute(status -> pcsCases.findByCaseReference(reference).orElseThrow()
            .getDocuments().iterator().next().getId());

        event(reference, pcsCase -> documents.findById(sourceId).orElseThrow().setFileName("Amended witness.pdf"));
        flags.update(flags.flag(FeatureFlag.CASE_BUNDLE_VIEW.key()).variationForAll(true));
        render(reference);
        assertThat(text(latest(reference))).contains("Amended witness.pdf");

        event(reference, pcsCase -> documents.deleteById(sourceId));
        render(reference);
        assertThat(text(latest(reference))).doesNotContain("Amended witness.pdf").doesNotContain("Content of");

        BundleTab tab = view(reference, "judge").getBundleTab();
        assertThat(tab.getPreviousVersions()).hasSize(versions(reference).size() - 1);
        assertThat(tab.getPreviousVersions()).extracting(version -> version.getValue().getBundle().getUrl())
            .doesNotContain(tab.getBundle().getUrl())
            .allSatisfy(url -> assertThat(events.documents().stored()).extracting(Document::getUrl).contains(url));
    }

    @Test
    void issuingACounterclaimAddsItsPreviouslyHiddenDocuments() throws Exception {
        long reference = issued();
        event(reference, pcsCase -> {
            var counterclaim = CounterClaimEntity.builder().party(pcsCase.getParties().iterator().next())
                .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED).build();
            pcsCase.addCounterClaim(counterclaim);
            var source = document("Counterclaim.pdf", CaseFileCategory.STATEMENTS_OF_CASE, LocalDate.of(2026, 1, 1));
            source.setCounterClaim(counterclaim);
            pcsCase.addDocuments(List.of(source));
        });
        render(reference);
        assertThat(text(latest(reference))).doesNotContain("Counterclaim.pdf");

        event(reference, pcsCase -> pcsCase.getCounterClaims().getFirst()
            .setStatus(CounterClaimState.COUNTER_CLAIM_ISSUED));
        render(reference);

        assertThat(text(latest(reference))).contains("Counterclaim.pdf");
    }

    @Test
    void missingSourcesAppearInTheJudgesTab() throws Exception {
        long reference = issued();
        var missing = document("Deleted.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        Document stored = events.documents().stored().stream()
            .filter(document -> document.getUrl().equals(missing.getUrl())).findFirst().orElseThrow();
        events.documents().delete(stored);
        event(reference, pcsCase -> pcsCase.addDocuments(List.of(missing)));
        render(reference);
        flags.update(flags.flag(FeatureFlag.CASE_BUNDLE_VIEW.key()).variationForAll(true));
        BundleTab tab = view(reference, "judge").getBundleTab();
        assertThat(tab.getStatus()).contains("Some documents could not be included");
        assertThat(tab.getMissingDocuments()).singleElement().satisfies(missingDocument -> {
            assertThat(missingDocument.getValue().getDocumentName()).isEqualTo("Deleted.pdf");
            assertThat(missingDocument.getValue().getFolder()).isEqualTo("Evidence");
            assertThat(missingDocument.getValue().getPage()).isPositive();
            assertThat(missingDocument.getValue().getReason())
                .isEqualTo("The document could not be found. It may have been deleted.");
        });
        int page = tab.getMissingDocuments().getFirst().getValue().getPage();
        assertThat(pageText(latest(reference), page)).contains("could not be found");
    }

    @Test
    void rendersInBackgroundButOnlyTargetedJudgesReceiveBundleFields() throws Exception {
        long reference = issued();
        event(reference, pcsCase -> pcsCase.addDocuments(List.of(
            document("Background.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1)))));
        render(reference);

        var allowed = events.registerActor("Selected", "Judge", "caseworker-pcs", "judge");
        final var other = events.registerActor("Other", "Judge", "caseworker-pcs", "judge");
        var admin = events.registerActor("Selected", "Admin", "caseworker-pcs", "hearing-centre-admin");
        assertThat(events.view(reference, allowed).getBundleTab()).isIn(null, new BundleTab());
        String originalTitle = events.view(reference, allowed).getCaseTitleMarkdown();
        assertThat(originalTitle).doesNotContain("/documentsv2/");

        flags.update(flags.flag(FeatureFlag.CASE_BUNDLE_VIEW.key()).clearTargets().variationForAll(false)
            .variationForUser(allowed.uid(), true).variationForUser(admin.uid(), true));
        PCSCase visible = events.view(reference, allowed);
        assertThat(visible.getBundleTab().getBundle()).isNotNull();
        assertThat(visible.getCaseTitleMarkdown())
            .contains("href=\"/documentsv2/" + latest(reference).getDocumentId() + "/binary\"")
            .contains("target=\"_blank\" rel=\"noopener noreferrer\"")
            .contains("Whole file download (opens in new tab)");
        assertThat(events.view(reference, other).getBundleTab()).isIn(null, new BundleTab());
        assertThat(events.view(reference, other).getCaseTitleMarkdown()).isEqualTo(originalTitle);
        assertThat(events.view(reference, admin).getBundleTab()).isIn(null, new BundleTab());
        assertThat(events.view(reference, admin).getCaseTitleMarkdown()).isEqualTo(originalTitle);

        flags.update(flags.flag(FeatureFlag.CASE_BUNDLE_VIEW.key()).clearTargets().variationForAll(false));
        assertThat(events.view(reference, allowed).getBundleTab()).isIn(null, new BundleTab());
        assertThat(events.view(reference, allowed).getCaseTitleMarkdown()).isEqualTo(originalTitle);
    }

    @Test
    void overviewHasNoDownloadUntilTheBundleIsMade() throws Exception {
        long reference = issued();
        var judge = events.registerActor("Selected", "Judge", "caseworker-pcs", "judge");
        flags.update(flags.flag(FeatureFlag.CASE_BUNDLE_VIEW.key()).variationForUser(judge.uid(), true));

        assertThat(events.view(reference, judge).getBundleTab()).isIn(null, new BundleTab());
        assertThat(events.view(reference, judge).getCaseTitleMarkdown()).doesNotContain("/documentsv2/");
        render(reference);
        assertThat(events.view(reference, judge).getCaseTitleMarkdown()).contains("/documentsv2/");
    }

    /**
     * An issued case after its first event. The fixture seeds cases without an event, as no real case is made, so
     * nothing has built its bundle until an event runs.
     */
    private long issued() {
        long reference = cases.issue();
        event(reference, pcsCase -> { });
        return reference;
    }

    private void event(long reference, Consumer<PcsCaseEntity> change) {
        systemEvents.execute(reference, UUID.randomUUID(), context -> {
            change.accept(pcsCases.findByCaseReference(reference).orElseThrow());
            return SystemEventResult.withoutStateTransition("bundleTestChange", "Bundle test change");
        });
    }

    /** Runs the worker until the case's bundle has nothing left to render. */
    private void render(long reference) {
        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofMillis(50)).until(() -> {
            worker.worker().poll();
            return jdbc.queryForObject("select count(*) from bundling.doc_job where case_reference = ?"
                + " and state in ('QUEUED', 'IN_PROGRESS')", Integer.class, reference) == 0;
        });
    }

    private long caseRevision(long reference) {
        return jdbc.queryForObject("select case_revision from ccd.case_data where reference = ?", Long.class,
                                   reference);
    }

    private List<CaseBundleEntity> versions(long reference) {
        return versions.findByCaseIdOrderByCaseRevisionDesc(
            pcsCases.findByCaseReference(reference).orElseThrow().getId());
    }

    private CaseBundleEntity latest(long reference) {
        return versions(reference).getFirst();
    }

    private Document current(long reference) {
        return stored(latest(reference));
    }

    private Document stored(CaseBundleEntity version) {
        return events.documents().stored().stream().filter(document -> document.getUrl().equals(version.getUrl()))
            .findFirst().orElseThrow();
    }

    private String text(CaseBundleEntity version) throws IOException {
        return text(stored(version));
    }

    private String text(Document document) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(events.documents().content(document))) {
            return new PDFTextStripper().getText(pdf);
        }
    }

    private String pageText(CaseBundleEntity version, int page) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(events.documents().content(stored(version)))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            return stripper.getText(pdf);
        }
    }

    private OrderStart.CaseBundle orderBundle(long reference, String role) {
        return events.external(reference, MakeOrder.MAKE_ORDER)
            .as(events.registerActor("Bundle", "Reader", "caseworker-pcs", role)).start().caseContext().caseBundle();
    }

    private PCSCase view(long reference, String role) {
        return events.view(reference, events.registerActor("Bundle", "Reader", "caseworker-pcs", role));
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
