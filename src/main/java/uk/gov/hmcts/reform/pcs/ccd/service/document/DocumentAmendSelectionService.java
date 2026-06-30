package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@Service
public class DocumentAmendSelectionService {

    private static final String SELECT_DIFFERENT_FOLDER_ERROR = "Select a different folder to continue";
    private static final String SELECT_DOCUMENT_ERROR = "Select which document you want to amend";
    private static final Comparator<DocumentEntity> DOCUMENT_ORDER = Comparator
        .comparing(DocumentEntity::getSubmittedDate, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(DocumentEntity::getFileName, Comparator.nullsLast(String::compareToIgnoreCase));

    private final PcsCaseService pcsCaseService;
    private final AddressFormatter addressFormatter;
    private final CaseNameFormatter caseNameFormatter;

    public DocumentAmendSelectionService(PcsCaseService pcsCaseService, AddressFormatter addressFormatter,
                                         CaseNameFormatter caseNameFormatter) {
        this.pcsCaseService = pcsCaseService;
        this.addressFormatter = addressFormatter;
        this.caseNameFormatter = caseNameFormatter;
    }

    public void initialise(long caseReference, PCSCase caseData) {
        DocumentAmendDetails details = getOrCreateDetails(caseData);
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);

        details.setPropertyAddressSummary(addressFormatter.formatShortAddress(caseData.getPropertyAddress(),
                                                                              COMMA_DELIMITER));
        details.setPartyNamesSummary(buildPartyNamesSummary(caseData));
        setDocumentsForCategory(details, pcsCase, CaseFileCategory.STATEMENTS_OF_CASE);
        setDocumentsForCategory(details, pcsCase, CaseFileCategory.PROPERTY_DOCUMENTS);
        setDocumentsForCategory(details, pcsCase, CaseFileCategory.EVIDENCE);
        setDocumentsForCategory(details, pcsCase, CaseFileCategory.HEARING_DOCUMENTS);
        setDocumentsForCategory(details, pcsCase, CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS);
        setDocumentsForCategory(details, pcsCase, CaseFileCategory.APPLICATIONS);
        setDocumentsForCategory(details, pcsCase, CaseFileCategory.APPEALS);
        setDocumentsForCategory(details, pcsCase, CaseFileCategory.CORRESPONDENCE);
        setDocumentsForCategory(details, pcsCase, CaseFileCategory.UNCATEGORISED_DOCUMENTS);
    }

    public List<String> validateAndStoreSelection(PCSCase caseData) {
        DocumentAmendDetails details = caseData.getDocumentAmendDetails();
        if (details == null || details.getSelectedFolder() == null) {
            return List.of();
        }

        CaseFileCategory selectedFolder = details.getSelectedFolder().getCategory();
        DynamicStringList selectedDocuments = documentsForCategory(details, selectedFolder);
        details.setSelectedFolderId(selectedFolder.getId());
        details.setSelectedFolderLabel(selectedFolder.getLabel());

        if (isEmpty(selectedDocuments)) {
            details.setSelectedDocumentId(null);
            details.setSelectedDocumentFileName(null);
            return List.of(SELECT_DIFFERENT_FOLDER_ERROR);
        }

        DynamicStringListElement selectedDocument = selectedDocuments.getValue();
        if (selectedDocument == null || selectedDocument.getCode() == null) {
            details.setSelectedDocumentId(null);
            details.setSelectedDocumentFileName(null);
            return List.of(SELECT_DOCUMENT_ERROR);
        }

        details.setSelectedDocumentId(selectedDocument.getCode());
        details.setSelectedDocumentFileName(selectedDocument.getLabel());
        return List.of();
    }

    private void setDocumentsForCategory(DocumentAmendDetails details, PcsCaseEntity pcsCase,
                                         CaseFileCategory category) {
        DynamicStringList documents = documentList(pcsCase, category, documentsForCategory(details, category));
        applyDocumentsForCategory(details, category, documents);
        setEmptyForCategory(details, category, YesOrNo.from(isEmpty(documents)));
    }

    private DynamicStringList documentList(PcsCaseEntity pcsCase, CaseFileCategory category,
                                           DynamicStringList existingList) {
        DynamicStringListElement selected = existingList == null ? null : existingList.getValue();

        List<DynamicStringListElement> documents = CollectionUtils.isEmpty(pcsCase.getDocuments())
            ? List.of()
            : pcsCase.getDocuments().stream()
                .filter(Objects::nonNull)
                .filter(document -> isInCategory(document, category))
                .sorted(DOCUMENT_ORDER)
                .map(document -> DynamicStringListElement.builder()
                    .code(document.getId().toString())
                    .label(document.getFileName())
                    .build())
                .toList();

        return DynamicStringList.builder()
            .value(retainSelectedValue(selected, documents))
            .listItems(documents)
            .build();
    }

    private boolean isInCategory(DocumentEntity document, CaseFileCategory category) {
        return category.getId().equals(document.getCategoryId());
    }

    private DynamicStringListElement retainSelectedValue(DynamicStringListElement selected,
                                                         List<DynamicStringListElement> options) {
        if (selected == null || selected.getCode() == null) {
            return null;
        }

        return options.stream()
            .filter(option -> selected.getCode().equals(option.getCode()))
            .findFirst()
            .orElse(null);
    }

    private boolean isEmpty(DynamicStringList documents) {
        return documents == null || CollectionUtils.isEmpty(documents.getListItems());
    }

    private String buildPartyNamesSummary(PCSCase caseData) {
        if (caseData.getClaimantNames() != null && caseData.getDefendantNames() != null) {
            return caseData.getClaimantNames() + " vs " + caseData.getDefendantNames();
        }
        if (CollectionUtils.isEmpty(caseData.getAllClaimants())
            && CollectionUtils.isEmpty(caseData.getAllDefendants())) {
            return null;
        }
        return caseNameFormatter.formatCaseName(caseData);
    }

    private DocumentAmendDetails getOrCreateDetails(PCSCase caseData) {
        if (caseData.getDocumentAmendDetails() == null) {
            caseData.setDocumentAmendDetails(new DocumentAmendDetails());
        }
        return caseData.getDocumentAmendDetails();
    }

    private DynamicStringList documentsForCategory(DocumentAmendDetails details, CaseFileCategory category) {
        return switch (category) {
            case STATEMENTS_OF_CASE -> details.getStatementsOfCaseDocuments();
            case PROPERTY_DOCUMENTS -> details.getPropertyDocuments();
            case EVIDENCE -> details.getEvidenceDocuments();
            case HEARING_DOCUMENTS -> details.getHearingDocuments();
            case ORDERS_AND_NOTICE_OF_HEARINGS -> details.getOrdersAndNoticeOfHearingsDocuments();
            case APPLICATIONS -> details.getApplicationsDocuments();
            case APPEALS -> details.getAppealsDocuments();
            case CORRESPONDENCE -> details.getCorrespondenceDocuments();
            case UNCATEGORISED_DOCUMENTS -> details.getUncategorisedDocuments();
        };
    }

    private void applyDocumentsForCategory(DocumentAmendDetails details, CaseFileCategory category,
                                           DynamicStringList documents) {
        switch (category) {
            case STATEMENTS_OF_CASE -> details.setStatementsOfCaseDocuments(documents);
            case PROPERTY_DOCUMENTS -> details.setPropertyDocuments(documents);
            case EVIDENCE -> details.setEvidenceDocuments(documents);
            case HEARING_DOCUMENTS -> details.setHearingDocuments(documents);
            case ORDERS_AND_NOTICE_OF_HEARINGS -> details.setOrdersAndNoticeOfHearingsDocuments(documents);
            case APPLICATIONS -> details.setApplicationsDocuments(documents);
            case APPEALS -> details.setAppealsDocuments(documents);
            case CORRESPONDENCE -> details.setCorrespondenceDocuments(documents);
            case UNCATEGORISED_DOCUMENTS -> details.setUncategorisedDocuments(documents);
        }
    }

    private void setEmptyForCategory(DocumentAmendDetails details, CaseFileCategory category, YesOrNo empty) {
        switch (category) {
            case STATEMENTS_OF_CASE -> details.setStatementsOfCaseEmpty(empty);
            case PROPERTY_DOCUMENTS -> details.setPropertyDocumentsEmpty(empty);
            case EVIDENCE -> details.setEvidenceEmpty(empty);
            case HEARING_DOCUMENTS -> details.setHearingDocumentsEmpty(empty);
            case ORDERS_AND_NOTICE_OF_HEARINGS -> details.setOrdersAndNoticeOfHearingsEmpty(empty);
            case APPLICATIONS -> details.setApplicationsEmpty(empty);
            case APPEALS -> details.setAppealsEmpty(empty);
            case CORRESPONDENCE -> details.setCorrespondenceEmpty(empty);
            case UNCATEGORISED_DOCUMENTS -> details.setUncategorisedDocumentsEmpty(empty);
        }
    }
}
