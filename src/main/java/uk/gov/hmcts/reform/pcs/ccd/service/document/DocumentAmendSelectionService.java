package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentListService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@Service
public class DocumentAmendSelectionService {

    public static final String SELECT_DIFFERENT_FOLDER_ERROR = "Select a different folder to continue";
    private static final Comparator<DocumentEntity> DOCUMENT_ORDER = Comparator
        .comparing(DocumentEntity::getSubmittedDate, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(DocumentEntity::getFileName, Comparator.nullsLast(String::compareToIgnoreCase));
    private static final String PARTY_POSTFIX_PATTERN = " - (Claimant|Defendant) \\d+$";
    private static final String GEN_APP_POSTFIX_PATTERN = " GA\\d+$";
    private static final String ISSUE_DATE_POSTFIX_PATTERN = " \\d{8}$";

    private final PcsCaseService pcsCaseService;
    private final AddressFormatter addressFormatter;
    private final CaseworkerDocumentListService caseworkerDocumentListService;

    public DocumentAmendSelectionService(PcsCaseService pcsCaseService,
                                         AddressFormatter addressFormatter,
                                         CaseworkerDocumentListService caseworkerDocumentListService) {
        this.pcsCaseService = pcsCaseService;
        this.addressFormatter = addressFormatter;
        this.caseworkerDocumentListService = caseworkerDocumentListService;
    }

    public void initialise(long caseReference, PCSCase caseData) {
        DocumentAmendDetails details = getOrCreateDetails(caseData);
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);

        details.setPropertyAddressSummary(addressFormatter.formatShortAddress(caseData.getPropertyAddress(),
                                                                              COMMA_DELIMITER));
        for (CaseFileCategory category : CaseFileCategory.values()) {
            setDocumentsForCategory(details, pcsCase, category);
        }
        details.setRelatedParty(caseworkerDocumentListService.buildRelatedPartyList(
            pcsCase,
            details.getRelatedParty()
        ));
        setApplicationOrCounterclaimLists(caseData, details, pcsCase);
    }

    public List<String> validateAndStoreSelection(long caseReference, PCSCase caseData) {
        DocumentAmendDetails details = caseData.getDocumentAmendDetails();
        if (details == null || details.getSelectedFolder() == null) {
            return List.of();
        }

        CaseFileCategory selectedFolder = details.getSelectedFolder();
        DynamicList selectedDocuments = documentsForCategory(details, selectedFolder);
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
        DocumentEntity selectedDocumentEntity = resolveSelectedDocumentEntity(
            caseReference,
            selectedFolder,
            resolvedDocument
        );
        String selectedDocumentBaseFileName = editableBaseFileName(resolvedDocument.getLabel());
        details.setSelectedDocumentId(resolvedDocument.getCode().toString());
        details.setSelectedDocumentFileName(resolvedDocument.getLabel());
        details.setSelectedDocumentBaseFileName(selectedDocumentBaseFileName);
        details.setAmendedFileName(selectedDocumentBaseFileName);
        details.setSelectedDocumentIssueDate(
            selectedDocumentEntity == null ? null : selectedDocumentEntity.getIssueDate()
        );
        details.setIssueDate(selectedDocumentEntity == null ? null : selectedDocumentEntity.getIssueDate());
        preselectRelatedParty(details, selectedDocumentEntity);
        return List.of();
    }

    private void clearSelectedDocument(DocumentAmendDetails details) {
        details.setSelectedDocumentId(null);
        details.setSelectedDocumentFileName(null);
        details.setSelectedDocumentBaseFileName(null);
        details.setAmendedFileName(null);
        details.setSelectedDocumentIssueDate(null);
        details.setIssueDate(null);
        if (details.getRelatedParty() != null) {
            details.getRelatedParty().setValue(null);
        }
    }

    private void preselectRelatedParty(DocumentAmendDetails details, DocumentEntity selectedDocumentEntity) {
        if (selectedDocumentEntity == null
            || selectedDocumentEntity.getParty() == null
            || selectedDocumentEntity.getParty().getId() == null
            || details.getRelatedParty() == null) {
            return;
        }

        String selectedPartyId = selectedDocumentEntity.getParty().getId().toString();
        details.getRelatedParty().getListItems().stream()
            .filter(option -> option.getCode() != null && selectedPartyId.equals(option.getCode().toString()))
            .findFirst()
            .ifPresent(details.getRelatedParty()::setValue);
    }

    private void setApplicationOrCounterclaimLists(PCSCase caseData, DocumentAmendDetails details,
                                                  PcsCaseEntity pcsCase) {
        List<ListValue<GeneralApplication>> genApps = caseData.getGenApps();
        List<CounterClaimEntity> counterClaims = pcsCase.getCounterClaims();
        boolean showRelatedSubmissionsList = caseworkerDocumentListService
            .hasRelatedSubmissions(genApps, counterClaims);

        details.setShowRelatedSubmissionsList(VerticalYesNo.from(showRelatedSubmissionsList));
        details.setRelatedSubmission(showRelatedSubmissionsList
            ? caseworkerDocumentListService.buildRelatedSubmissionsList(
                pcsCase,
                genApps,
                counterClaims,
                details.getRelatedSubmission())
            : null);

        details.setRelatedSubmissionsDocumentType(caseworkerDocumentListService.buildDocumentTypeList(
            caseData.getLegislativeCountry(),
            details.getRelatedSubmissionsDocumentType()));
        details.setStandaloneDocumentType(caseworkerDocumentListService.buildDocumentTypeList(
            caseData.getLegislativeCountry(),
            details.getStandaloneDocumentType()));
    }

    private DocumentEntity resolveSelectedDocumentEntity(long caseReference,
                                                         CaseFileCategory selectedFolder,
                                                         DynamicListElement selectedDocument) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        if (CollectionUtils.isEmpty(pcsCase.getDocuments()) || selectedDocument.getCode() == null) {
            return null;
        }

        String selectedDocumentId = selectedDocument.getCode().toString();
        return pcsCase.getDocuments().stream()
            .filter(Objects::nonNull)
            .filter(document -> isInCategory(document, selectedFolder))
            .filter(document -> selectedDocumentId.equals(document.getId().toString()))
            .findFirst()
            .orElse(null);
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

    private String editableBaseFileName(String fileName) {
        if (fileName == null) {
            return null;
        }

        String baseFileName;
        int extensionSeparator = fileName.lastIndexOf('.');
        if (extensionSeparator <= 0) {
            baseFileName = fileName;
        } else {
            baseFileName = fileName.substring(0, extensionSeparator);
        }

        return baseFileName
            .replaceFirst(PARTY_POSTFIX_PATTERN, "")
            .replaceFirst(GEN_APP_POSTFIX_PATTERN, "")
            .replaceFirst(ISSUE_DATE_POSTFIX_PATTERN, "");
    }

    private void setDocumentsForCategory(DocumentAmendDetails details, PcsCaseEntity pcsCase,
                                         CaseFileCategory category) {
        DynamicList documents = documentList(pcsCase, category, documentsForCategory(details, category));
        applyDocumentsForCategory(details, category, documents);
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

    private DynamicListElement retainSelectedValue(DynamicListElement selected,
                                                  List<DynamicListElement> options) {
        if (selected == null || selected.getCode() == null) {
            if (selected == null || selected.getLabel() == null) {
                return null;
            }
            return options.stream()
                .filter(option -> selected.getLabel().equals(option.getLabel()))
                .findFirst()
                .orElse(null);
        }

        return options.stream()
            .filter(option -> selected.getCode().toString().equals(option.getCode().toString())
                || selected.getLabel() != null && selected.getLabel().equals(option.getLabel()))
            .findFirst()
            .orElse(null);
    }

    private boolean isEmpty(DynamicList documents) {
        return documents == null || CollectionUtils.isEmpty(documents.getListItems());
    }

    private DocumentAmendDetails getOrCreateDetails(PCSCase caseData) {
        if (caseData.getDocumentAmendDetails() == null) {
            caseData.setDocumentAmendDetails(new DocumentAmendDetails());
        }
        return caseData.getDocumentAmendDetails();
    }

    private DynamicList documentsForCategory(DocumentAmendDetails details, CaseFileCategory category) {
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
                                           DynamicList documents) {
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
