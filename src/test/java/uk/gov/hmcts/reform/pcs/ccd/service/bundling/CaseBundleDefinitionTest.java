package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocSource;
import uk.gov.hmcts.ccd.sdk.doctoolkit.MissingDocuments;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PageNumbers;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PdfPart;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PdfRequest;
import uk.gov.hmcts.ccd.sdk.doctoolkit.PdfSection;
import uk.gov.hmcts.ccd.sdk.doctoolkit.jobs.DocRequestJson;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
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

class CaseBundleDefinitionTest {

    @Test
    void shouldMakeOneFolderPerCaseFileCategoryInDisplayOrder() {
        PdfRequest request = CaseBundleDefinition.build(caseWith());

        assertThat(request.fileName()).isEqualTo("1234567812345678-case-bundle.pdf");
        assertThat(request.root().sections()).extracting(PdfSection::title)
            .containsExactlyElementsOf(Arrays.stream(CaseFileCategory.values())
                .sorted(Comparator.comparingInt(CaseFileCategory::getDisplayOrder))
                .map(CaseFileCategory::getLabel)
                .toList());
        assertThat(request.root().sections()).extracting(PdfSection::placeholderWhenEmpty)
            .containsOnly(true);
        assertThat(request.missingDocuments()).isEqualTo(MissingDocuments.PLACEHOLDER);
    }

    @Test
    void shouldPresentTheBundleAsTheCourtDefault() {
        PdfRequest request = CaseBundleDefinition.build(caseWith());

        assertThat(request.title()).isEqualTo("Case bundle");
        assertThat(request.titlePage()).isTrue();
        assertThat(request.tableOfContents()).isTrue();
        assertThat(request.sectionCoverSheets()).isTrue();
        assertThat(request.documentCoverSheets()).isFalse();
        assertThat(request.pageNumbers()).isEqualTo(PageNumbers.BOTTOM_CENTRE_N_OF_M);
        assertThat(request.confidentialMarking()).isTrue();
        assertThat(request.category()).contains(CaseBundleDefinition.FOLDER_ID);
    }

    @Test
    void shouldBuildTheSameRequestFromTheSameCase() {
        DocumentEntity first = document("first.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        DocumentEntity second = document("second.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        GenAppEntity application = issuedApp(1, GenAppType.ADJOURN);
        DocumentEntity applicationForm =
            document("form.pdf", CaseFileCategory.APPLICATIONS, LocalDate.of(2026, 1, 1));
        applicationForm.setGeneralApplication(application);

        // The fingerprint that decides whether to re-render is of the stored request, so it must not vary.
        assertThat(DocRequestJson.write(CaseBundleDefinition.build(caseWith(first, second, applicationForm))))
            .isEqualTo(DocRequestJson.write(CaseBundleDefinition.build(caseWith(applicationForm, second, first))));
    }

    @Test
    void shouldOrderDocumentsChronologicallyWithinTheirFolder() {
        DocumentEntity late = document("late.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 3, 1));
        DocumentEntity early = document("early.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        DocumentEntity undated = document("undated.pdf", CaseFileCategory.EVIDENCE, null);
        undated.setSubmittedDate(Instant.parse("2026-02-01T10:00:00Z"));
        DocumentEntity claim = document("claim.pdf", CaseFileCategory.STATEMENTS_OF_CASE, LocalDate.of(2026, 5, 1));

        PdfRequest request = CaseBundleDefinition.build(caseWith(late, early, undated, claim));

        assertThat(titles(request, "Evidence")).containsExactly("early.pdf", "undated.pdf", "late.pdf");
        assertThat(titles(request, "Statements of case")).containsExactly("claim.pdf");
        PdfPart first = section(request, "Evidence").parts().getFirst();
        assertThat(first.source()).isEqualTo(DocSource.cdam(early.getDocumentId()));
        assertThat(first.id()).contains(early.getId().toString());
    }

    @Test
    void shouldLeaveOutAccessCodeAndNonCdamDocuments() {
        final DocumentEntity kept = document("kept.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        DocumentEntity accessCode = document("code.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        accessCode.setType(DocumentType.DEFENDANT_ACCESS_CODE);

        DocumentEntity nonCdam = document("external.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        nonCdam.setDocumentId(null);

        PdfRequest request = CaseBundleDefinition.build(caseWith(kept, accessCode, nonCdam));

        assertThat(request.allParts()).extracting(PdfPart::title).containsExactly("kept.pdf");
    }

    @Test
    void shouldFileAnUnknownCategoryAsUncategorised() {
        DocumentEntity stray = document("stray.pdf", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        stray.setCategoryId("somethingElse");

        PdfRequest request = CaseBundleDefinition.build(caseWith(stray));

        assertThat(titles(request, "Uncategorised documents")).containsExactly("stray.pdf");
    }

    @Test
    void shouldTitleAnUnnamedDocumentByItsType() {
        DocumentEntity typed = document(" ", CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 1));
        typed.setType(DocumentType.WITNESS_STATEMENT);
        DocumentEntity untyped = document(null, CaseFileCategory.EVIDENCE, LocalDate.of(2026, 1, 2));

        PdfRequest request = CaseBundleDefinition.build(caseWith(typed, untyped));

        assertThat(titles(request, "Evidence")).containsExactly("Witness statement", "Document");
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

        PdfRequest request =
            CaseBundleDefinition.build(caseWith(issuedApp, pendingApp, withoutNotice, draftCounterclaim));

        assertThat(request.allParts()).extracting(PdfPart::title)
            .containsExactly("issued-app.pdf", "without-notice.pdf");
    }

    @Test
    void shouldPutEachApplicationsDocumentsUnderItsReferenceAfterTheRestOfTheFolder() {
        GenAppEntity second = issuedApp(2, GenAppType.SUSPEND);
        GenAppEntity first = issuedApp(1, GenAppType.ADJOURN);
        final DocumentEntity loose = document("letter.pdf", CaseFileCategory.APPLICATIONS, LocalDate.of(2026, 3, 1));
        DocumentEntity suspendForm = document("suspend.pdf", CaseFileCategory.APPLICATIONS, LocalDate.of(2026, 2, 1));
        suspendForm.setGeneralApplication(second);
        DocumentEntity adjournLate = document("adjourn-2.pdf", CaseFileCategory.APPLICATIONS, LocalDate.of(2026, 1, 9));
        adjournLate.setGeneralApplication(first);
        DocumentEntity adjournEarly =
            document("adjourn-1.pdf", CaseFileCategory.APPLICATIONS, LocalDate.of(2026, 1, 2));
        adjournEarly.setGeneralApplication(first);

        PdfRequest request = CaseBundleDefinition.build(caseWith(loose, suspendForm, adjournLate, adjournEarly));

        PdfSection applications = section(request, "Applications");
        assertThat(titles(request, "Applications")).containsExactly("letter.pdf");
        assertThat(applications.sections()).extracting(PdfSection::title)
            .containsExactly("GA1: " + GenAppType.ADJOURN.getShortName(), "GA2: " + GenAppType.SUSPEND.getShortName());
        assertThat(applications.sections().getFirst().parts()).extracting(PdfPart::title)
            .containsExactly("adjourn-1.pdf", "adjourn-2.pdf");
        assertThat(applications.sections().get(1).parts()).extracting(PdfPart::title)
            .containsExactly("suspend.pdf");
    }

    @Test
    void shouldFetchRecordingsRatherThanLinkToDmStore() {
        DocumentEntity audio = document("hearing.mp3", CaseFileCategory.HEARING_DOCUMENTS, null);
        audio.setContentType("audio/mpeg");
        DocumentEntity video = document("inspection.mov", CaseFileCategory.HEARING_DOCUMENTS, null);
        video.setContentType("video/quicktime");

        PdfRequest request = CaseBundleDefinition.build(caseWith(audio, video));

        // Fetched, so an unsupported recording becomes a missing-document page rather than failing the bundle.
        assertThat(section(request, "Hearing documents").parts())
            .hasSize(2).allSatisfy(part -> assertThat(part.source()).isInstanceOf(DocSource.Cdam.class));
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

    private static PdfSection section(PdfRequest request, String title) {
        return request.root().sections().stream().filter(s -> s.title().equals(title)).findFirst().orElseThrow();
    }

    private static List<String> titles(PdfRequest request, String folder) {
        return section(request, folder).parts().stream().map(PdfPart::title).toList();
    }

    private static GenAppEntity issuedApp(int rank, GenAppType type) {
        GenAppEntity genApp = genApp(GenAppState.GEN_APP_ISSUED, VerticalYesNo.NO);
        genApp.setRank(rank);
        genApp.setType(type);
        return genApp;
    }

    private static GenAppEntity genApp(GenAppState state, VerticalYesNo withoutNotice) {
        GenAppEntity genApp = new GenAppEntity();
        genApp.setState(state);
        genApp.setWithoutNotice(withoutNotice);
        return genApp;
    }
}
