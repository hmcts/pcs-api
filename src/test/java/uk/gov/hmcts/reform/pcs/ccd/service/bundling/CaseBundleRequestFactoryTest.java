package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleDocument;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleRequest;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleSection;
import uk.gov.hmcts.ccd.sdk.bundling.api.EmptySectionPolicy;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CaseBundleRequestFactoryTest {

    private static final UUID JOB_ID = UUID.randomUUID();

    private final CaseBundleRequestFactory underTest = new CaseBundleRequestFactory();

    @Test
    void shouldMakeOneFolderPerCaseFileCategoryInDisplayOrder() {
        BundleRequest request = underTest.build(JOB_ID, caseWith());

        assertThat(request.externalId()).isEqualTo(JOB_ID);
        assertThat(request.fileName()).isEqualTo("1234567812345678-case-bundle.pdf");
        assertThat(request.root().sections()).extracting(BundleSection::title)
            .containsExactlyElementsOf(Arrays.stream(CaseFileCategory.values())
                .sorted(Comparator.comparingInt(CaseFileCategory::getDisplayOrder))
                .map(CaseFileCategory::getLabel)
                .toList());
        assertThat(request.root().sections()).extracting(BundleSection::emptySectionPolicy)
            .containsOnly(EmptySectionPolicy.INCLUDE_PLACEHOLDER);
    }

    @Test
    void shouldOrderDocumentsChronologicallyWithinTheirFolder() {
        DocumentEntity late = document("late.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 3, 1));
        DocumentEntity early = document("early.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        DocumentEntity undated = document("undated.pdf", CaseFileCategory.EVIDENCE, null);
        undated.setSubmittedDate(Instant.parse("2026-02-01T10:00:00Z"));
        DocumentEntity claim = document("claim.pdf", CaseFileCategory.STATEMENTS_OF_CASE, LocalDate.of(2026, 5, 1));

        BundleRequest request = underTest.build(JOB_ID, caseWith(late, early, undated, claim));

        assertThat(titles(request, "Evidence")).containsExactly("early.pdf", "undated.pdf", "late.pdf");
        assertThat(titles(request, "Statements of case")).containsExactly("claim.pdf");
        BundleDocument first = section(request, "Evidence").documents().getFirst();
        assertThat(first.reference().provider()).isEqualTo(CaseBundleScope.CDAM_PROVIDER);
        assertThat(first.reference().id()).isEqualTo(early.getDocumentId().toString());
    }

    @Test
    void shouldLeaveOutRemovedAccessCodeAndBundleDocuments() {
        final DocumentEntity kept = document("kept.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        DocumentEntity removed = document("removed.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        removed.setRemoved(true);
        DocumentEntity accessCode = document("code.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        accessCode.setType(DocumentType.DEFENDANT_ACCESS_CODE);
        DocumentEntity bundle = document("bundle.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        bundle.setType(DocumentType.CASE_BUNDLE);

        BundleRequest request = underTest.build(JOB_ID, caseWith(kept, removed, accessCode, bundle));

        assertThat(request.allDocuments()).extracting(BundleDocument::title).containsExactly("kept.pdf");
    }

    @Test
    void shouldFileAnUnknownCategoryAsUncategorised() {
        DocumentEntity stray = document("stray.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        stray.setCategoryId("somethingElse");

        BundleRequest request = underTest.build(JOB_ID, caseWith(stray));

        assertThat(titles(request, "Uncategorised documents")).containsExactly("stray.pdf");
    }

    @Test
    void shouldLinkToAudioRatherThanFetchIt() {
        DocumentEntity recording = document("hearing.mp3", CaseFileCategory.HEARING_DOCUMENTS, null);
        recording.setContentType("audio/mpeg");

        BundleRequest request = underTest.build(JOB_ID, caseWith(recording));

        assertThat(section(request, "Hearing documents").documents().getFirst().media()).hasValueSatisfying(
            media -> assertThat(media.accessUrl()).isEqualTo(recording.getBinaryUrl()));
    }

    @Test
    void shouldTitleAnUnnamedDocumentByItsType() {
        DocumentEntity typed = document(" ", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        typed.setType(DocumentType.WITNESS_STATEMENT);
        DocumentEntity untyped = document(null, CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 2));

        BundleRequest request = underTest.build(JOB_ID, caseWith(typed, untyped));

        assertThat(titles(request, "Evidence")).containsExactly("Witness statement", "Document");
    }

    private static PcsCaseEntity caseWith(DocumentEntity... documents) {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setCaseReference(1234567812345678L);
        pcsCase.addDocuments(List.of(documents));
        return pcsCase;
    }

    private static DocumentEntity document(String fileName, CaseFileCategory category, LocalDate issueDate) {
        UUID cdamId = UUID.randomUUID();
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .documentId(cdamId)
            .fileName(fileName)
            .categoryId(category.getId())
            .binaryUrl("http://cdam/cases/documents/" + cdamId + "/binary")
            .issueDate(issueDate)
            .submittedDate(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
    }

    private static BundleSection section(BundleRequest request, String title) {
        return request.root().sections().stream().filter(s -> s.title().equals(title)).findFirst().orElseThrow();
    }

    private static List<String> titles(BundleRequest request, String folder) {
        return section(request, folder).documents().stream().map(BundleDocument::title).toList();
    }

    @Test
    void shouldLeaveOutDocumentsOfUnissuedApplicationsAndCounterclaims() {
        DocumentEntity issuedApp = document("issued-app.pdf", CaseFileCategory.APPLICATIONS, LocalDate.of(2026, 1, 1));
        issuedApp.setGeneralApplication(genApp(GenAppState.GEN_APP_ISSUED, VerticalYesNo.NO));
        DocumentEntity pendingApp =
            document("pending-app.pdf", CaseFileCategory.APPLICATIONS, LocalDate.of(2026, 1, 2));
        pendingApp.setGeneralApplication(genApp(GenAppState.PENDING_GEN_APP_ISSUED, VerticalYesNo.NO));
        DocumentEntity withoutNotice = document("without-notice.pdf", CaseFileCategory.APPLICATIONS,
                                                LocalDate.of(2026, 1, 3));
        withoutNotice.setGeneralApplication(genApp(GenAppState.PENDING_GEN_APP_ISSUED, VerticalYesNo.YES));
        DocumentEntity draftCounterclaim = document("counterclaim.pdf", CaseFileCategory.STATEMENTS_OF_CASE,
                                                    LocalDate.of(2026, 1, 4));
        CounterClaimEntity counterClaim = new CounterClaimEntity();
        counterClaim.setStatus(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED);
        draftCounterclaim.setCounterClaim(counterClaim);

        BundleRequest request =
            underTest.build(JOB_ID, caseWith(issuedApp, pendingApp, withoutNotice, draftCounterclaim));

        assertThat(request.allDocuments()).extracting(BundleDocument::title)
            .containsExactly("issued-app.pdf", "without-notice.pdf");
    }

    @Test
    void shouldLeaveOutRecordingsWithNoLink() {
        DocumentEntity recording = document("hearing.mp3", CaseFileCategory.HEARING_DOCUMENTS, null);
        recording.setContentType("audio/mpeg");
        recording.setBinaryUrl(" ");

        BundleRequest request = underTest.build(JOB_ID, caseWith(recording));

        assertThat(request.allDocuments()).isEmpty();
    }

    private static GenAppEntity genApp(GenAppState state, VerticalYesNo withoutNotice) {
        GenAppEntity genApp = new GenAppEntity();
        genApp.setState(state);
        genApp.setWithoutNotice(withoutNotice);
        return genApp;
    }
}
