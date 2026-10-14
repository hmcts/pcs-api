package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleDocument;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleErrorCode;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleOutcome;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleRequest;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleSection;
import uk.gov.hmcts.ccd.sdk.bundling.api.DocumentReference;
import uk.gov.hmcts.ccd.sdk.bundling.api.MissingDocument;
import uk.gov.hmcts.ccd.sdk.bundling.api.MissingDocumentReason;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJob;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobReport;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobState;
import uk.gov.hmcts.ccd.sdk.bundling.job.OutboxBundleJobService;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.BundleTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle.MissingBundleDocument;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.bundling.CaseBundleScope;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BundleTabViewTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final String COALESCE_KEY = CaseBundleScope.coalesceKey(CASE_ID);
    private static final UUID STORED_JOB = UUID.randomUUID();

    @Mock
    private ObjectProvider<OutboxBundleJobService> bundleJobsProvider;
    @Mock
    private OutboxBundleJobService bundleJobs;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(CASE_ID).build();
    private final PCSCase caseData = PCSCase.builder().build();

    private BundleTabView underTest;

    @BeforeEach
    void setUp() {
        underTest = new BundleTabView(bundleJobsProvider, new UploadTimestampProvider());
        lenient().when(bundleJobsProvider.getIfAvailable()).thenReturn(bundleJobs);
    }

    @Test
    void shouldLeaveTheTabHiddenWhileCaseBundlingIsSwitchedOff() {
        when(bundleJobsProvider.getIfAvailable()).thenReturn(null);

        underTest.setCaseFields(caseData, pcsCase);

        assertThat(caseData.getBundleTab()).isNull();
    }

    @Test
    void shouldLeaveTheTabHiddenBeforeABundleHasBeenAskedFor() {
        when(bundleJobs.findLatest(COALESCE_KEY)).thenReturn(Optional.empty());

        underTest.setCaseFields(caseData, pcsCase);

        assertThat(caseData.getBundleTab()).isNull();
    }

    @Test
    void shouldShowTheStoredBundleAndThatItIsUpToDate() {
        final DocumentEntity bundle = storedBundle();
        when(bundleJobs.findLatest(COALESCE_KEY)).thenReturn(Optional.of(job(STORED_JOB, BundleJobState.COMPLETED)));
        when(bundleJobs.findReport(STORED_JOB)).thenReturn(Optional.of(report(List.of())));

        underTest.setCaseFields(caseData, pcsCase);

        BundleTab tab = caseData.getBundleTab();
        assertThat(tab.getStatus()).isEqualTo(BundleTabView.UP_TO_DATE);
        assertThat(tab.getBundle().getUrl()).isEqualTo(bundle.getUrl());
        assertThat(tab.getBundle().getBinaryUrl()).isEqualTo(bundle.getBinaryUrl());
        assertThat(tab.getBundle().getFilename()).isEqualTo(bundle.getFileName());
        assertThat(tab.getBundle().getCategoryId()).isEqualTo(CaseBundleScope.BUNDLES_CATEGORY_ID);
        assertThat(tab.getLastUpdated()).isEqualTo("6 October 2026 at 3:05pm");
        assertThat(tab.getPages()).isEqualTo(42);
        assertThat(tab.getMissingDocuments()).isNull();
    }

    @Test
    void shouldListTheMissingDocumentsWithTheirFolderPageAndReason() {
        storedBundle();
        when(bundleJobs.findLatest(COALESCE_KEY)).thenReturn(Optional.of(job(STORED_JOB, BundleJobState.COMPLETED)));
        when(bundleJobs.findReport(STORED_JOB)).thenReturn(Optional.of(report(List.of(
            missing("witness", MissingDocumentReason.UNREADABLE, 12),
            missing("unknown", MissingDocumentReason.NOT_FOUND, 30)))));

        underTest.setCaseFields(caseData, pcsCase);

        BundleTab tab = caseData.getBundleTab();
        assertThat(tab.getStatus()).isEqualTo(BundleTabView.UP_TO_DATE_WITH_MISSING);
        assertThat(tab.getMissingDocuments()).extracting(ListValue::getValue).containsExactly(
            new MissingBundleDocument("Witness statement.pdf", "Evidence", 12,
                                      MissingDocumentReason.UNREADABLE.message()),
            new MissingBundleDocument("unknown", null, 30, MissingDocumentReason.NOT_FOUND.message()));
        assertThat(tab.getMissingDocuments()).extracting(ListValue::getId).containsExactly("witness", "unknown");
    }

    @ParameterizedTest
    @CsvSource({
        "QUEUED, " + BundleTabView.UPDATING,
        "IN_PROGRESS, " + BundleTabView.UPDATING,
        "FAILED, " + BundleTabView.UPDATE_FAILED,
        "COMPLETED, " + BundleTabView.UP_TO_DATE
    })
    void shouldSayWhetherANewerBundleIsOnItsWay(BundleJobState newerJobState, String status) {
        storedBundle();
        when(bundleJobs.findLatest(COALESCE_KEY))
            .thenReturn(Optional.of(job(UUID.randomUUID(), newerJobState)));
        when(bundleJobs.findReport(STORED_JOB)).thenReturn(Optional.of(report(List.of())));

        underTest.setCaseFields(caseData, pcsCase);

        assertThat(caseData.getBundleTab().getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @CsvSource({
        "QUEUED, " + BundleTabView.PREPARING,
        "IN_PROGRESS, " + BundleTabView.PREPARING,
        "FAILED, " + BundleTabView.CREATE_FAILED,
        "COMPLETED, " + BundleTabView.PREPARING
    })
    void shouldSayWhatIsHappeningToTheFirstBundle(BundleJobState state, String status) {
        when(bundleJobs.findLatest(COALESCE_KEY)).thenReturn(Optional.of(job(UUID.randomUUID(), state)));

        underTest.setCaseFields(caseData, pcsCase);

        BundleTab tab = caseData.getBundleTab();
        assertThat(tab.getStatus()).isEqualTo(status);
        assertThat(tab.getBundle()).isNull();
        assertThat(tab.getPages()).isNull();
        assertThat(tab.getMissingDocuments()).isNull();
    }

    @Test
    void shouldShowTheBundleWithoutDetailsWhenItsJobHasNoReport() {
        storedBundle();
        when(bundleJobs.findLatest(COALESCE_KEY)).thenReturn(Optional.empty());
        when(bundleJobs.findReport(STORED_JOB)).thenReturn(Optional.empty());

        underTest.setCaseFields(caseData, pcsCase);

        BundleTab tab = caseData.getBundleTab();
        assertThat(tab.getStatus()).isEqualTo(BundleTabView.UP_TO_DATE);
        assertThat(tab.getBundle()).isNotNull();
        assertThat(tab.getPages()).isNull();
    }

    private DocumentEntity storedBundle() {
        DocumentEntity bundle = DocumentEntity.builder()
            .id(UUID.randomUUID())
            .type(DocumentType.CASE_BUNDLE)
            .categoryId(CaseBundleScope.BUNDLES_CATEGORY_ID)
            .url("http://cdam/cases/documents/bundle")
            .binaryUrl("http://cdam/cases/documents/bundle/binary")
            .fileName("1234-case-bundle.pdf")
            .submittedDate(Instant.parse("2026-10-06T14:05:00Z"))
            .bundleJobId(STORED_JOB)
            .build();
        DocumentEntity evidence = DocumentEntity.builder().id(UUID.randomUUID()).type(DocumentType.WITNESS_STATEMENT)
            .build();
        pcsCase.setDocuments(new ArrayList<>(List.of(evidence, bundle)));
        return bundle;
    }

    private static BundleJob job(UUID id, BundleJobState state) {
        Instant now = Instant.now();
        return new BundleJob(id, state, 1, now, now, Optional.of(COALESCE_KEY), 0, Optional.of(now),
                             Optional.empty(), Optional.empty());
    }

    private static MissingDocument missing(String documentId, MissingDocumentReason reason, int page) {
        return new MissingDocument(documentId, new DocumentReference(CaseBundleScope.CDAM_PROVIDER, documentId),
                                   reason, BundleErrorCode.DOCUMENT_INSPECTION_FAILED, "detail", page);
    }

    private static BundleJobReport report(List<MissingDocument> missing) {
        BundleRequest request = BundleRequest.builder()
            .externalId(STORED_JOB)
            .title("Case bundle")
            .fileName("1234-case-bundle.pdf")
            .root(BundleSection.builder("Case bundle")
                      .section(BundleSection.builder("Evidence")
                                   .document(BundleDocument.builder().id("witness").title("Witness statement.pdf")
                                                 .reference(new DocumentReference("cdam", "witness")).build())
                                   .build())
                      .build())
            .build();
        return new BundleJobReport(request, BundleOutcome.COMPLETED, "1234-case-bundle.pdf", 100, "sha", 42,
                                   List.of(), missing, List.of());
    }
}
