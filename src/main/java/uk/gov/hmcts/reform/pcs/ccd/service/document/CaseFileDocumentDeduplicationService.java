package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentWithId;
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

@Service
public class CaseFileDocumentDeduplicationService {

    public void removeDocumentsAlreadyPresentInOtherCaseFields(PCSCase pcsCase) {
        List<ListValue<Document>> allDocuments = pcsCase.getAllDocuments();

        if (allDocuments == null || allDocuments.isEmpty()) {
            return;
        }

        Set<String> documentIdsInOtherFields = findDocumentIdsOutsideAllDocuments(pcsCase);

        pcsCase.setAllDocuments(allDocuments.stream()
                                    .filter(document -> !documentIdsInOtherFields.contains(document.getId()))
                                    .toList());
    }

    private Set<String> findDocumentIdsOutsideAllDocuments(PCSCase pcsCase) {
        Set<String> documentIds = new HashSet<>();

        addDocumentIdsFromRentArrears(pcsCase.getRentArrears(), documentIds);
        addDocumentIdsFromNoticeServedDetails(pcsCase.getNoticeServedDetails(), documentIds);
        addDocumentIdsFromTenancyLicenceDetails(pcsCase.getTenancyLicenceDetails(), documentIds);
        addDocumentIdsFromRequiredDocumentsWales(pcsCase.getRequiredDocumentsWales(), documentIds);
        addDocumentIdsFromOccupationLicenceDetailsWales(pcsCase.getOccupationLicenceDetailsWales(), documentIds);
        addDocumentIdsFromGenApps(pcsCase.getGenApps(), documentIds);
        addDocumentIdsFromCaseDetailsTab(pcsCase.getCaseDetailsTab(), documentIds);

        return documentIds;
    }

    private void addDocumentIdsFromRentArrears(RentArrearsSection rentArrears, Set<String> documentIds) {
        if (rentArrears != null) {
            addDocumentIds(rentArrears.getStatementDocuments(), documentIds);
        }
    }

    private void addDocumentIdsFromNoticeServedDetails(
        uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails noticeServedDetails,
        Set<String> documentIds
    ) {
        if (noticeServedDetails != null) {
            addDocumentIds(noticeServedDetails.getDocuments(), documentIds);
        }
    }

    private void addDocumentIdsFromTenancyLicenceDetails(
        TenancyLicenceDetails tenancyLicenceDetails,
        Set<String> documentIds
    ) {
        if (tenancyLicenceDetails != null) {
            addDocumentIds(tenancyLicenceDetails.getTenancyLicenceDocuments(), documentIds);
        }
    }

    private void addDocumentIdsFromRequiredDocumentsWales(WalesDocuments requiredDocumentsWales,
                                                          Set<String> documentIds) {
        if (requiredDocumentsWales != null) {
            addDocumentIds(requiredDocumentsWales.getEnergyPerformance(), documentIds);
            addDocumentIds(requiredDocumentsWales.getGasSafetyReport(), documentIds);
            addDocumentIds(requiredDocumentsWales.getElectricalInstallation(), documentIds);
        }
    }

    private void addDocumentIdsFromOccupationLicenceDetailsWales(
        OccupationLicenceDetailsWales occupationLicenceDetailsWales,
        Set<String> documentIds
    ) {
        if (occupationLicenceDetailsWales != null) {
            addDocumentIds(occupationLicenceDetailsWales.getLicenceDocuments(), documentIds);
        }
    }

    private void addDocumentIdsFromGenApps(List<ListValue<GeneralApplication>> genApps, Set<String> documentIds) {
        if (genApps == null) {
            return;
        }

        genApps.stream()
            .map(ListValue::getValue)
            .forEach(genApp -> {
                if (genApp != null) {
                    addDocumentId(genApp.getSubmissionDocument(), documentIds);
                    addDocumentIds(genApp.getSupportingDocuments(), documentIds);
                }
            });
    }

    private void addDocumentIdsFromCaseDetailsTab(CaseDetailsTab caseDetailsTab, Set<String> documentIds) {
        if (caseDetailsTab == null) {
            return;
        }

        addDocumentIdsFromRentArrearsTabDetails(caseDetailsTab.getRentArrearsDetails(), documentIds);
        addDocumentIdsFromNoticeTabDetails(caseDetailsTab.getNoticeDetails(), documentIds);
        addDocumentIdsFromTenancyLicenceTabDetails(caseDetailsTab.getTenancyLicenceDetails(), documentIds);
        addDocumentIdsFromRequiredDocumentsTabDetails(caseDetailsTab.getRequiredDocumentsDetails(), documentIds);
        addDocumentIdsFromOccupationContractOrLicenceTabDetails(
            caseDetailsTab.getOccupationContractLicenceDetails(),
            documentIds
        );
    }

    private void addDocumentIdsFromRentArrearsTabDetails(RentArrearsTabDetails rentArrearsDetails,
                                                         Set<String> documentIds) {
        if (rentArrearsDetails != null) {
            addDocumentIds(rentArrearsDetails.getRentStatement(), documentIds);
        }
    }

    private void addDocumentIdsFromNoticeTabDetails(NoticeTabDetails noticeDetails, Set<String> documentIds) {
        if (noticeDetails != null) {
            addDocumentIds(noticeDetails.getNoticeDocuments(), documentIds);
        }
    }

    private void addDocumentIdsFromTenancyLicenceTabDetails(TenancyLicenceTabDetails tenancyLicenceDetails,
                                                            Set<String> documentIds) {
        if (tenancyLicenceDetails != null) {
            addDocumentIds(tenancyLicenceDetails.getTenancyLicenceDocuments(), documentIds);
        }
    }

    private void addDocumentIdsFromRequiredDocumentsTabDetails(RequiredDocumentsTabDetails requiredDocumentsDetails,
                                                               Set<String> documentIds) {
        if (requiredDocumentsDetails != null) {
            addDocumentIds(requiredDocumentsDetails.getEnergyPerformanceCertificates(), documentIds);
            addDocumentIds(requiredDocumentsDetails.getGasSafetyReports(), documentIds);
            addDocumentIds(requiredDocumentsDetails.getElectricalInstallationReports(), documentIds);
        }
    }

    private void addDocumentIdsFromOccupationContractOrLicenceTabDetails(
        OccupationContractOrLicenceTabDetails occupationContractLicenceDetails,
        Set<String> documentIds
    ) {
        if (occupationContractLicenceDetails != null) {
            addDocumentIds(occupationContractLicenceDetails.getDocuments(), documentIds);
        }
    }

    private void addDocumentIds(List<ListValue<Document>> documents, Set<String> documentIds) {
        if (documents == null) {
            return;
        }

        documents.stream()
            .map(ListValue::getId)
            .forEach(documentId -> addDocumentId(documentId, documentIds));
    }

    private void addDocumentId(DocumentWithId documentWithId, Set<String> documentIds) {
        if (documentWithId != null) {
            addDocumentId(documentWithId.getId(), documentIds);
        }
    }

    private void addDocumentId(String documentId, Set<String> documentIds) {
        if (documentId != null) {
            documentIds.add(documentId);
        }
    }
}
