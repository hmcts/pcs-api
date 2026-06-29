package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentAmendSelectionService {

    private static final String SELECT_DIFFERENT_FOLDER_ERROR = "Select a different folder to continue";
    private static final Comparator<DocumentEntity> DOCUMENT_ORDER = Comparator
        .comparing(DocumentEntity::getSubmittedDate, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(DocumentEntity::getFileName, Comparator.nullsLast(String::compareToIgnoreCase));

    private final PcsCaseService pcsCaseService;

    public DocumentAmendSelectionService(PcsCaseService pcsCaseService) {
        this.pcsCaseService = pcsCaseService;
    }

    public static UUID folderCode(CaseFileCategory category) {
        return UUID.nameUUIDFromBytes(category.getId().getBytes(StandardCharsets.UTF_8));
    }

    public void initialise(long caseReference, PCSCase caseData) {
        DocumentAmendDetails details = getOrCreateDetails(caseData);
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);

        details.setSelectedFolder(folderList(details.getSelectedFolder()));
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
        if (details == null || selectedFolderId(details) == null) {
            return List.of();
        }

        CaseFileCategory selectedFolder = categoryForId(selectedFolderId(details)).orElse(null);
        if (selectedFolder == null) {
            return List.of();
        }

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
            return List.of();
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
                .filter(this::isVisibleToCaseworker)
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

    private boolean isVisibleToCaseworker(DocumentEntity document) {
        return document.getGeneralApplication() == null
            || document.getGeneralApplication().getWithoutNotice() != VerticalYesNo.YES;
    }

    private boolean isInCategory(DocumentEntity document, CaseFileCategory category) {
        if (category == CaseFileCategory.UNCATEGORISED_DOCUMENTS) {
            return document.getCategoryId() == null;
        }

        return category.getId().equals(document.getCategoryId());
    }

    private DynamicList folderList(DynamicList existingList) {
        DynamicListElement selected = existingList == null ? null : existingList.getValue();
        List<DynamicListElement> folders = List.of(CaseFileCategory.values()).stream()
            .sorted(Comparator.comparing(CaseFileCategory::getDisplayOrder))
            .map(category -> new DynamicListElement(folderCode(category), category.getLabel()))
            .toList();

        return new DynamicList(retainSelectedFolder(selected, folders), folders);
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

    private String selectedFolderId(DocumentAmendDetails details) {
        if (details.getSelectedFolder() == null || details.getSelectedFolder().getValue() == null) {
            return null;
        }

        UUID selectedCode = details.getSelectedFolder().getValue().getCode();
        return List.of(CaseFileCategory.values()).stream()
            .filter(category -> folderCode(category).equals(selectedCode))
            .map(CaseFileCategory::getId)
            .findFirst()
            .orElse(null);
    }

    private DynamicListElement retainSelectedFolder(DynamicListElement selected, List<DynamicListElement> options) {
        if (selected == null || selected.getCode() == null) {
            return null;
        }

        return options.stream()
            .filter(option -> selected.getCode().equals(option.getCode()))
            .findFirst()
            .orElse(null);
    }

    private Optional<CaseFileCategory> categoryForId(String id) {
        return List.of(CaseFileCategory.values()).stream()
            .filter(category -> category.getId().equals(id))
            .findFirst();
    }

    private boolean isEmpty(DynamicStringList documents) {
        return documents == null || CollectionUtils.isEmpty(documents.getListItems());
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
