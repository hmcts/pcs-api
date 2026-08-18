package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.springframework.stereotype.Service;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class CaseFileDocumentDeduplicationService {

    public void removeDocumentsAlreadyPresentInOtherCaseFields(PCSCase pcsCase) {
        List<ListValue<Document>> allDocuments = pcsCase.getAllDocuments();

        if (allDocuments == null || allDocuments.isEmpty()) {
            return;
        }

        Set<String> documentReferencesInOtherFields = findDocumentReferencesOutsideAllDocuments(pcsCase);

        pcsCase.setAllDocuments(allDocuments.stream()
                                    .filter(document -> allDocumentReferences(document)
                                        .noneMatch(documentReferencesInOtherFields::contains))
                                    .toList());
    }

    private Set<String> findDocumentReferencesOutsideAllDocuments(PCSCase pcsCase) {
        Set<String> documentReferences = new HashSet<>();

        addDocumentReferencesFromRentArrears(pcsCase.getRentArrears(), documentReferences);
        addDocumentReferencesFromNoticeServedDetails(pcsCase.getNoticeServedDetails(), documentReferences);
        addDocumentReferencesFromTenancyLicenceDetails(pcsCase.getTenancyLicenceDetails(), documentReferences);
        addDocumentReferencesFromRequiredDocumentsWales(pcsCase.getRequiredDocumentsWales(), documentReferences);
        addDocumentReferencesFromOccupationLicenceDetailsWales(
            pcsCase.getOccupationLicenceDetailsWales(),
            documentReferences
        );
        addDocumentReferencesFromGenApps(pcsCase.getGenApps(), documentReferences);
        addDocumentReferencesFromCaseDetailsTab(pcsCase.getCaseDetailsTab(), documentReferences);
        return documentReferences;
    }

    private void addDocumentReferencesFromRentArrears(RentArrearsSection rentArrears, Set<String> documentReferences) {
        if (rentArrears != null) {
            addDocumentReferences(rentArrears.getStatementDocuments(), documentReferences);
        }
    }

    private void addDocumentReferencesFromNoticeServedDetails(
        NoticeServedDetails noticeServedDetails,
        Set<String> documentReferences
    ) {
        if (noticeServedDetails != null) {
            addDocumentReferences(noticeServedDetails.getDocuments(), documentReferences);
        }
    }

    private void addDocumentReferencesFromTenancyLicenceDetails(
        TenancyLicenceDetails tenancyLicenceDetails,
        Set<String> documentReferences
    ) {
        if (tenancyLicenceDetails != null) {
            addDocumentReferences(tenancyLicenceDetails.getTenancyLicenceDocuments(), documentReferences);
        }
    }

    private void addDocumentReferencesFromRequiredDocumentsWales(WalesDocuments requiredDocumentsWales,
                                                                Set<String> documentReferences) {
        if (requiredDocumentsWales != null) {
            addDocumentReferences(requiredDocumentsWales.getEnergyPerformance(), documentReferences);
            addDocumentReferences(requiredDocumentsWales.getGasSafetyReport(), documentReferences);
            addDocumentReferences(requiredDocumentsWales.getElectricalInstallation(), documentReferences);
        }
    }

    private void addDocumentReferencesFromOccupationLicenceDetailsWales(
        OccupationLicenceDetailsWales occupationLicenceDetailsWales,
        Set<String> documentReferences
    ) {
        if (occupationLicenceDetailsWales != null) {
            addDocumentReferences(occupationLicenceDetailsWales.getLicenceDocuments(), documentReferences);
        }
    }

    private void addDocumentReferencesFromGenApps(List<ListValue<GeneralApplication>> genApps,
                                                  Set<String> documentReferences) {
        if (genApps == null) {
            return;
        }

        genApps.stream()
            .map(ListValue::getValue)
            .forEach(genApp -> addDocumentReferencesFromGenApp(genApp, documentReferences));
    }

    private void addDocumentReferencesFromGenApp(GeneralApplication genApp, Set<String> documentReferences) {
        if (genApp == null) {
            return;
        }

        addDocumentReferences(genApp.getSubmissionDocument(), documentReferences);
        addDocumentReferences(genApp.getSupportingDocuments(), documentReferences);
    }

    private void addDocumentReferencesFromCaseDetailsTab(CaseDetailsTab caseDetailsTab,
                                                         Set<String> documentReferences) {
        if (caseDetailsTab == null) {
            return;
        }

        addDocumentReferencesFromRentArrearsTabDetails(caseDetailsTab.getRentArrearsDetails(), documentReferences);
        addDocumentReferencesFromNoticeTabDetails(caseDetailsTab.getNoticeDetails(), documentReferences);
        addDocumentReferencesFromTenancyLicenceTabDetails(
            caseDetailsTab.getTenancyLicenceDetails(),
            documentReferences
        );
        addDocumentReferencesFromRequiredDocumentsTabDetails(
            caseDetailsTab.getRequiredDocumentsDetails(),
            documentReferences
        );
        addDocumentReferencesFromOccupationContractOrLicenceTabDetails(
            caseDetailsTab.getOccupationContractLicenceDetails(),
            documentReferences
        );
    }

    private void addDocumentReferencesFromRentArrearsTabDetails(RentArrearsTabDetails rentArrearsDetails,
                                                               Set<String> documentReferences) {
        if (rentArrearsDetails != null) {
            addDocumentReferences(rentArrearsDetails.getRentStatement(), documentReferences);
        }
    }

    private void addDocumentReferencesFromNoticeTabDetails(NoticeTabDetails noticeDetails,
                                                          Set<String> documentReferences) {
        if (noticeDetails != null) {
            addDocumentReferences(noticeDetails.getNoticeDocuments(), documentReferences);
        }
    }

    private void addDocumentReferencesFromTenancyLicenceTabDetails(TenancyLicenceTabDetails tenancyLicenceDetails,
                                                                  Set<String> documentReferences) {
        if (tenancyLicenceDetails != null) {
            addDocumentReferences(tenancyLicenceDetails.getTenancyLicenceDocuments(), documentReferences);
        }
    }

    private void addDocumentReferencesFromRequiredDocumentsTabDetails(
        RequiredDocumentsTabDetails requiredDocumentsDetails,
        Set<String> documentReferences
    ) {
        if (requiredDocumentsDetails != null) {
            addDocumentReferences(requiredDocumentsDetails.getEnergyPerformanceCertificates(), documentReferences);
            addDocumentReferences(requiredDocumentsDetails.getGasSafetyReports(), documentReferences);
            addDocumentReferences(requiredDocumentsDetails.getElectricalInstallationReports(), documentReferences);
        }
    }

    private void addDocumentReferencesFromOccupationContractOrLicenceTabDetails(
        OccupationContractOrLicenceTabDetails occupationContractLicenceDetails,
        Set<String> documentReferences
    ) {
        if (occupationContractLicenceDetails != null) {
            addDocumentReferences(occupationContractLicenceDetails.getDocuments(), documentReferences);
        }
    }

    private void addDocumentReferences(List<ListValue<Document>> documents, Set<String> documentReferences) {
        if (documents == null) {
            return;
        }

        documents.stream()
            .forEach(document -> documentReferences(document)
                .forEach(reference -> addDocumentReference(reference, documentReferences)));
    }

    private void addDocumentReferences(Document document, Set<String> documentReferences) {
        documentReferences(document)
            .forEach(reference -> addDocumentReference(reference, documentReferences));
    }

    private void addDocumentReferences(DocumentWithId documentWithId, Set<String> documentReferences) {
        if (documentWithId == null) {
            return;
        }

        addDocumentReference(documentWithId.getId(), documentReferences);
        addDocumentReferences(documentWithId.getDocument(), documentReferences);
    }

    private Stream<String> documentReferences(ListValue<Document> document) {
        if (document == null) {
            return Stream.empty();
        }

        return Stream.concat(
            Stream.of(document.getId()),
            documentReferences(document.getValue())
        );
    }

    private Stream<String> documentReferences(Document document) {
        if (document == null) {
            return Stream.empty();
        }

        return Stream.of(document.getUrl(), document.getBinaryUrl());
    }

    private Stream<String> allDocumentReferences(ListValue<Document> document) {
        if (document == null) {
            return Stream.empty();
        }

        return documentReferences(document);
    }

    private void addDocumentReference(String documentReference, Set<String> documentReferences) {
        if (documentReference != null) {
            documentReferences.add(documentReference);
        }
    }
}
