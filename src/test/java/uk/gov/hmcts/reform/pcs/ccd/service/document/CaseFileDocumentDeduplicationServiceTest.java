package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentWithId;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.CaseDetailsTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.RequiredDocumentsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.TenancyLicenceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.OccupationContractOrLicenceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaseFileDocumentDeduplicationServiceTest {

    private final CaseFileDocumentDeduplicationService underTest = new CaseFileDocumentDeduplicationService();

    @Test
    void shouldKeepDocumentInAllDocumentsWhenItDoesNotAppearInAnotherCaseField() {
        // Given
        ListValue<Document> document = documentListValue("document-id", "genApps.docx");
        PCSCase pcsCase = PCSCase.builder()
            .allDocuments(List.of(document))
            .build();

        // When
        underTest.removeDocumentsAlreadyPresentInOtherCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(document);
    }

    @Test
    void shouldRemoveRentStatementFromAllDocumentsWhenItAppearsInRentArrears() {
        // Given
        ListValue<Document> rentStatement = documentListValue("rent-statement-id", "rent-statement.pdf");
        ListValue<Document> otherDocument = documentListValue("other-document-id", "genApps.docx");
        PCSCase pcsCase = PCSCase.builder()
            .allDocuments(List.of(rentStatement, otherDocument))
            .rentArrears(RentArrearsSection.builder()
                             .statementDocuments(List.of(rentStatement))
                             .build())
            .build();

        // When
        underTest.removeDocumentsAlreadyPresentInOtherCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(otherDocument);
    }

    @Test
    void shouldRemoveGenAppDocumentsFromAllDocumentsWhenTheyAppearInGenApps() {
        // Given
        ListValue<Document> submissionDocument = documentListValue("submission-document-id", "general-application.pdf");
        ListValue<Document> supportingDocument = documentListValue("supporting-document-id", "genApps.docx");
        ListValue<Document> otherDocument = documentListValue("other-document-id", "other.pdf");
        PCSCase pcsCase = PCSCase.builder()
            .allDocuments(List.of(submissionDocument, supportingDocument, otherDocument))
            .genApps(List.of(ListValue.<GeneralApplication>builder()
                                .value(GeneralApplication.builder()
                                           .submissionDocument(DocumentWithId.builder()
                                                                   .id(submissionDocument.getId())
                                                                   .build())
                                           .supportingDocuments(List.of(genAppDocumentListValue(supportingDocument)))
                                           .build())
                                .build()))
            .build();

        // When
        underTest.removeDocumentsAlreadyPresentInOtherCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(otherDocument);
    }

    @Test
    void shouldRemoveGenAppDocumentsFromAllDocumentsWhenTheyHaveDifferentIdsButSameUrls() {
        // Given
        Document submissionDocument = Document.builder()
            .filename("General Application GA1 - Defendant 1.pdf")
            .url("http://dm-store/documents/submission")
            .binaryUrl("http://dm-store/documents/submission/binary")
            .build();
        Document supportingDocument = Document.builder()
            .filename("genApps GA1 - Defendant 1.docx")
            .url("http://dm-store/documents/supporting")
            .binaryUrl("http://dm-store/documents/supporting/binary")
            .build();

        ListValue<Document> duplicateSubmissionDocument = ListValue.<Document>builder()
            .id("duplicate-submission-document-id")
            .value(submissionDocument)
            .build();
        ListValue<Document> duplicateSupportingDocument = ListValue.<Document>builder()
            .id("duplicate-supporting-document-id")
            .value(supportingDocument)
            .build();
        ListValue<Document> sameFilenameDifferentDocument = documentListValue(
            "other-supporting-document-id",
            "other-genApps.docx"
        );

        PCSCase pcsCase = PCSCase.builder()
            .allDocuments(List.of(
                duplicateSubmissionDocument,
                duplicateSupportingDocument,
                sameFilenameDifferentDocument
            ))
            .genApps(List.of(ListValue.<GeneralApplication>builder()
                                .value(GeneralApplication.builder()
                                           .submissionDocument(DocumentWithId.builder()
                                                                   .id("submission-document-id")
                                                                   .document(submissionDocument)
                                                                   .build())
                                           .supportingDocuments(List.of(ListValue.<Document>builder()
                                                                           .id("supporting-document-id")
                                                                           .value(supportingDocument)
                                                                           .build()))
                                           .build())
                                .build()))
            .build();

        // When
        underTest.removeDocumentsAlreadyPresentInOtherCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(sameFilenameDifferentDocument);
    }

    @Test
    void shouldKeepGenAppDocumentInAllDocumentsWhenOnlyGenAppFilenameMatches() {
        // Given
        ListValue<Document> allDocumentsSupportingDocument = ListValue.<Document>builder()
            .id("all-documents-supporting-document-id")
            .value(Document.builder()
                       .filename("genApps GA1 - Defendant 1.docx")
                       .url("http://dm-store/documents/supporting")
                       .binaryUrl("http://dm-store/documents/supporting/binary")
                       .build())
            .build();
        ListValue<Document> otherDocument = documentListValue("other-document-id", "other.pdf");
        ListValue<Document> genAppSupportingDocument = ListValue.<Document>builder()
            .id("gen-app-supporting-document-id")
            .value(Document.builder()
                       .filename(" GENAPPS GA1 - DEFENDANT 1.DOCX ")
                       .build())
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .allDocuments(List.of(allDocumentsSupportingDocument, otherDocument))
            .genApps(List.of(ListValue.<GeneralApplication>builder()
                                .value(GeneralApplication.builder()
                                           .supportingDocuments(List.of(genAppSupportingDocument))
                                           .build())
                                .build()))
            .build();

        // When
        underTest.removeDocumentsAlreadyPresentInOtherCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(allDocumentsSupportingDocument, otherDocument);
    }

    @Test
    void shouldRemoveDocumentsThatAppearInSubmittedCaseDetailsFields() {
        // Given
        ListValue<Document> noticeDocument = documentListValue("notice-document-id", "notice.pdf");
        ListValue<Document> tenancyDocument = documentListValue("tenancy-document-id", "tenancy.pdf");
        ListValue<Document> epcDocument = documentListValue("epc-document-id", "epc.pdf");
        ListValue<Document> gasDocument = documentListValue("gas-document-id", "gas.pdf");
        ListValue<Document> eicrDocument = documentListValue("eicr-document-id", "eicr.pdf");
        ListValue<Document> occupationLicenceDocument = documentListValue(
            "occupation-licence-document-id",
            "occupation-licence.pdf"
        );
        ListValue<Document> otherDocument = documentListValue("other-document-id", "other.pdf");
        PCSCase pcsCase = PCSCase.builder()
            .allDocuments(List.of(
                noticeDocument,
                tenancyDocument,
                epcDocument,
                gasDocument,
                eicrDocument,
                occupationLicenceDocument,
                otherDocument
            ))
            .noticeServedDetails(NoticeServedDetails.builder()
                                     .documents(List.of(noticeDocument))
                                     .build())
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .tenancyLicenceDocuments(List.of(tenancyDocument))
                                       .build())
            .requiredDocumentsWales(WalesDocuments.builder()
                                        .energyPerformance(List.of(epcDocument))
                                        .gasSafetyReport(List.of(gasDocument))
                                        .electricalInstallation(List.of(eicrDocument))
                                        .build())
            .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                                               .licenceDocuments(List.of(occupationLicenceDocument))
                                               .build())
            .build();

        // When
        underTest.removeDocumentsAlreadyPresentInOtherCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(otherDocument);
    }

    @Test
    void shouldRemoveDocumentsThatAppearInCaseDetailsTabFields() {
        // Given
        ListValue<Document> rentStatement = documentListValue("rent-statement-id", "rent-statement.pdf");
        ListValue<Document> noticeDocument = documentListValue("notice-document-id", "notice.pdf");
        ListValue<Document> tenancyDocument = documentListValue("tenancy-document-id", "tenancy.pdf");
        ListValue<Document> epcDocument = documentListValue("epc-document-id", "epc.pdf");
        ListValue<Document> gasDocument = documentListValue("gas-document-id", "gas.pdf");
        ListValue<Document> eicrDocument = documentListValue("eicr-document-id", "eicr.pdf");
        ListValue<Document> occupationLicenceDocument = documentListValue(
            "occupation-licence-document-id",
            "occupation-licence.pdf"
        );
        ListValue<Document> otherDocument = documentListValue("other-document-id", "other.pdf");
        PCSCase pcsCase = PCSCase.builder()
            .allDocuments(List.of(
                rentStatement,
                noticeDocument,
                tenancyDocument,
                epcDocument,
                gasDocument,
                eicrDocument,
                occupationLicenceDocument,
                otherDocument
            ))
            .caseDetailsTab(CaseDetailsTab.builder()
                                .rentArrearsDetails(RentArrearsTabDetails.builder()
                                                        .rentStatement(List.of(rentStatement))
                                                        .build())
                                .noticeDetails(NoticeTabDetails.builder()
                                                   .noticeDocuments(List.of(noticeDocument))
                                                   .build())
                                .tenancyLicenceDetails(TenancyLicenceTabDetails.builder()
                                                           .tenancyLicenceDocuments(List.of(tenancyDocument))
                                                           .build())
                                .requiredDocumentsDetails(RequiredDocumentsTabDetails.builder()
                                                              .energyPerformanceCertificates(List.of(epcDocument))
                                                              .gasSafetyReports(List.of(gasDocument))
                                                              .electricalInstallationReports(List.of(eicrDocument))
                                                              .build())
                                .occupationContractLicenceDetails(
                                    OccupationContractOrLicenceTabDetails.builder()
                                        .documents(List.of(occupationLicenceDocument))
                                        .build()
                                )
                                .build())
            .build();

        // When
        underTest.removeDocumentsAlreadyPresentInOtherCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(otherDocument);
    }

    private static ListValue<Document> documentListValue(String id, String filename) {
        return ListValue.<Document>builder()
            .id(id)
            .value(Document.builder()
                       .filename(filename)
                       .build())
            .build();
    }

    private static ListValue<Document> genAppDocumentListValue(ListValue<Document> document) {
        return ListValue.<Document>builder()
            .id(document.getId())
            .value(Document.builder()
                       .filename(document.getValue().getFilename())
                       .build())
            .build();
    }
}
