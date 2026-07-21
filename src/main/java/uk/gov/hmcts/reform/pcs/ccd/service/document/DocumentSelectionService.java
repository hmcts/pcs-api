package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;
import static uk.gov.hmcts.reform.pcs.ccd.util.DynamicListUtils.retainSelectedValue;

@Service
public class DocumentSelectionService {

    public static final String SELECT_DIFFERENT_FOLDER_ERROR = "Select a different folder to continue";
    private static final Comparator<DocumentEntity> DOCUMENT_ORDER = Comparator
        .comparing(DocumentEntity::getSubmittedDate, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(DocumentEntity::getFileName, Comparator.nullsLast(String::compareToIgnoreCase));

    private final PcsCaseService pcsCaseService;
    private final AddressFormatter addressFormatter;

    public DocumentSelectionService(PcsCaseService pcsCaseService,
                                         AddressFormatter addressFormatter) {
        this.pcsCaseService = pcsCaseService;
        this.addressFormatter = addressFormatter;
    }

    public void initialise(long caseReference, PCSCase caseData, DocumentSelectionDetails details) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);

        details.setPropertyAddressSummary(addressFormatter.formatShortAddress(caseData.getPropertyAddress(),
                                                                              COMMA_DELIMITER));
        for (CaseFileCategory category : CaseFileCategory.values()) {
            setDocumentsForCategory(details, caseData, pcsCase, category);
        }
    }

    public List<String> validateAndStoreSelection(PCSCase caseData, DocumentSelectionDetails details) {
        if (details == null || details.getSelectedFolder() == null) {
            return List.of();
        }

        CaseFileCategory selectedFolder = details.getSelectedFolder();
        DynamicList selectedDocuments = documentsForCategory(caseData, selectedFolder);
        details.setSelectedFolderId(selectedFolder.getId());
        details.setSelectedFolderLabel(selectedFolder.getLabel());

        if (isEmpty(selectedDocuments)) {
            clearSelectedDocument(details);
            return List.of(SELECT_DIFFERENT_FOLDER_ERROR);
        }

        DynamicListElement selectedDocument = selectedDocuments.getValue();
        if (selectedDocument == null || isBlankSelection(selectedDocument)) {
            clearSelectedDocument(details);
            return List.of();
        }

        DynamicListElement resolvedDocument = resolveSelectedDocument(selectedDocuments, selectedDocument);
        details.setSelectedDocumentId(resolvedDocument.getCode().toString());
        details.setSelectedDocumentFileName(resolvedDocument.getLabel());
        return List.of();
    }

    private void clearSelectedDocument(DocumentSelectionDetails details) {
        details.setSelectedDocumentId(null);
        details.setSelectedDocumentFileName(null);
    }

    private DynamicListElement resolveSelectedDocument(DynamicList selectedDocuments,
                                                       DynamicListElement selectedDocument) {
        if (selectedDocument.getCode() != null && selectedDocument.getLabel() != null) {
            return selectedDocument;
        }

        return selectedDocuments.getListItems().stream()
            .filter(option -> isSelectedOption(selectedDocument, option))
            .findFirst()
            .orElse(selectedDocument);
    }

    private boolean isSelectedOption(DynamicListElement selectedDocument, DynamicListElement option) {
        if (selectedDocument.getCode() != null && option.getCode() != null
            && selectedDocument.getCode().toString().equals(option.getCode().toString())) {
            return true;
        }
        return selectedDocument.getLabel() != null && selectedDocument.getLabel().equals(option.getLabel());
    }

    private boolean isBlankSelection(DynamicListElement selectedDocument) {
        return selectedDocument.getCode() == null && selectedDocument.getLabel() == null;
    }

    private void setDocumentsForCategory(DocumentSelectionDetails details, PCSCase caseData, PcsCaseEntity pcsCase,
                                         CaseFileCategory category) {
        DynamicList documents = documentList(pcsCase, category, documentsForCategory(caseData, category));
        applyDocumentsForCategory(caseData, category, documents);
        setEmptyForCategory(details, category, YesOrNo.from(isEmpty(documents)));
    }

    private DynamicList documentList(PcsCaseEntity pcsCase, CaseFileCategory category,
                                     DynamicList existingList) {
        DynamicListElement selected = existingList == null ? null : existingList.getValue();

        List<DynamicListElement> documents = CollectionUtils.isEmpty(pcsCase.getDocuments())
            ? List.of()
            : pcsCase.getDocuments().stream()
                .filter(Objects::nonNull)
                .filter(document -> isInCategory(document, category))
                .filter(document -> document.getType() != DocumentType.DEFENDANT_ACCESS_CODE)
                .sorted(DOCUMENT_ORDER)
                .map(document -> DynamicListElement.builder()
                    .code(document.getId())
                    .label(document.getFileName())
                    .build())
                .toList();

        return DynamicList.builder()
            .value(retainSelectedValue(selected, documents))
            .listItems(documents)
            .build();
    }

    private boolean isInCategory(DocumentEntity document, CaseFileCategory category) {
        return category.getId().equals(document.getCategoryId());
    }

    private boolean isEmpty(DynamicList documents) {
        return documents == null || CollectionUtils.isEmpty(documents.getListItems());
    }

    private DynamicList documentsForCategory(PCSCase caseData, CaseFileCategory category) {
        return switch (category) {
            case STATEMENTS_OF_CASE -> caseData.getStatementsOfCaseDocuments();
            case PROPERTY_DOCUMENTS -> caseData.getPropertyDocuments();
            case EVIDENCE -> caseData.getEvidenceDocuments();
            case HEARING_DOCUMENTS -> caseData.getHearingDocuments();
            case ORDERS_AND_NOTICE_OF_HEARINGS -> caseData.getOrdersAndNoticeOfHearingsDocuments();
            case APPLICATIONS -> caseData.getApplicationsDocuments();
            case APPEALS -> caseData.getAppealsDocuments();
            case CORRESPONDENCE -> caseData.getCorrespondenceDocuments();
            case UNCATEGORISED_DOCUMENTS -> caseData.getUncategorisedDocuments();
        };
    }

    private void applyDocumentsForCategory(PCSCase caseData, CaseFileCategory category,
                                           DynamicList documents) {
        switch (category) {
            case STATEMENTS_OF_CASE -> caseData.setStatementsOfCaseDocuments(documents);
            case PROPERTY_DOCUMENTS -> caseData.setPropertyDocuments(documents);
            case EVIDENCE -> caseData.setEvidenceDocuments(documents);
            case HEARING_DOCUMENTS -> caseData.setHearingDocuments(documents);
            case ORDERS_AND_NOTICE_OF_HEARINGS -> caseData.setOrdersAndNoticeOfHearingsDocuments(documents);
            case APPLICATIONS -> caseData.setApplicationsDocuments(documents);
            case APPEALS -> caseData.setAppealsDocuments(documents);
            case CORRESPONDENCE -> caseData.setCorrespondenceDocuments(documents);
            case UNCATEGORISED_DOCUMENTS -> caseData.setUncategorisedDocuments(documents);
        }
    }

    private void setEmptyForCategory(DocumentSelectionDetails details, CaseFileCategory category, YesOrNo empty) {
        details.setEmptyForCategory(category, empty);
    }
}
