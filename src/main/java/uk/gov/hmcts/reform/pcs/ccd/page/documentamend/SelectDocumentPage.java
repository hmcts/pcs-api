package uk.gov.hmcts.reform.pcs.ccd.page.documentamend;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendFolder;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendSelectionService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class SelectDocumentPage implements CcdPageConfiguration {

    private static final String PAGE_ID = "selectDocument";
    private static final String FIELD_PREFIX = "documentAmend_";
    private static final String YES = "=\"Yes\"";
    private static final String NO = "=\"No\"";

    private final DocumentAmendSelectionService documentAmendSelectionService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(PAGE_ID, this::midEvent)
            .pageLabel("Select document")
            .label(PAGE_ID + "-caseReference", "Case number: ${[CASE_REFERENCE]}")
            .label(PAGE_ID + "-propertyAddress", "${documentAmend_PropertyAddressSummary}",
                   "documentAmend_PropertyAddressSummary!=\"\"")
            .label(PAGE_ID + "-partyNames", "${documentAmend_PartyNamesSummary}",
                   "documentAmend_PartyNamesSummary!=\"\"")
            .label(PAGE_ID + "-separator", "---")
            .complex(PCSCase::getDocumentAmendDetails)
                .readonly(DocumentAmendDetails::getPropertyAddressSummary, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getPartyNamesSummary, NEVER_SHOW, true)
                .mandatory(DocumentAmendDetails::getSelectedFolder)
                .mandatory(DocumentAmendDetails::getStatementsOfCaseDocuments,
                    documentsShowCondition(CaseFileCategory.STATEMENTS_OF_CASE))
                .label("statementsOfCaseNoDocumentsQuestion", "Which document do you want to amend?",
                    noDocumentsShowCondition(CaseFileCategory.STATEMENTS_OF_CASE))
                .label("statementsOfCaseNoDocuments", noDocumentsMessage(CaseFileCategory.STATEMENTS_OF_CASE),
                    noDocumentsShowCondition(CaseFileCategory.STATEMENTS_OF_CASE))
                .mandatory(DocumentAmendDetails::getPropertyDocuments,
                    documentsShowCondition(CaseFileCategory.PROPERTY_DOCUMENTS))
                .label("propertyDocumentsNoDocumentsQuestion", "Which document do you want to amend?",
                    noDocumentsShowCondition(CaseFileCategory.PROPERTY_DOCUMENTS))
                .label("propertyDocumentsNoDocuments", noDocumentsMessage(CaseFileCategory.PROPERTY_DOCUMENTS),
                    noDocumentsShowCondition(CaseFileCategory.PROPERTY_DOCUMENTS))
                .mandatory(DocumentAmendDetails::getEvidenceDocuments,
                    documentsShowCondition(CaseFileCategory.EVIDENCE))
                .label("evidenceNoDocumentsQuestion", "Which document do you want to amend?",
                    noDocumentsShowCondition(CaseFileCategory.EVIDENCE))
                .label("evidenceNoDocuments", noDocumentsMessage(CaseFileCategory.EVIDENCE),
                    noDocumentsShowCondition(CaseFileCategory.EVIDENCE))
                .mandatory(DocumentAmendDetails::getHearingDocuments,
                    documentsShowCondition(CaseFileCategory.HEARING_DOCUMENTS))
                .label("hearingDocumentsNoDocumentsQuestion", "Which document do you want to amend?",
                    noDocumentsShowCondition(CaseFileCategory.HEARING_DOCUMENTS))
                .label("hearingDocumentsNoDocuments", noDocumentsMessage(CaseFileCategory.HEARING_DOCUMENTS),
                    noDocumentsShowCondition(CaseFileCategory.HEARING_DOCUMENTS))
                .mandatory(DocumentAmendDetails::getOrdersAndNoticeOfHearingsDocuments,
                    documentsShowCondition(CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS))
                .label("ordersAndNoticeOfHearingsNoDocumentsQuestion", "Which document do you want to amend?",
                    noDocumentsShowCondition(CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS))
                .label("ordersAndNoticeOfHearingsNoDocuments",
                    noDocumentsMessage(CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS),
                    noDocumentsShowCondition(CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS))
                .mandatory(DocumentAmendDetails::getApplicationsDocuments,
                    documentsShowCondition(CaseFileCategory.APPLICATIONS))
                .label("applicationsNoDocumentsQuestion", "Which document do you want to amend?",
                    noDocumentsShowCondition(CaseFileCategory.APPLICATIONS))
                .label("applicationsNoDocuments", noDocumentsMessage(CaseFileCategory.APPLICATIONS),
                    noDocumentsShowCondition(CaseFileCategory.APPLICATIONS))
                .mandatory(DocumentAmendDetails::getAppealsDocuments,
                    documentsShowCondition(CaseFileCategory.APPEALS))
                .label("appealsNoDocumentsQuestion", "Which document do you want to amend?",
                    noDocumentsShowCondition(CaseFileCategory.APPEALS))
                .label("appealsNoDocuments", noDocumentsMessage(CaseFileCategory.APPEALS),
                    noDocumentsShowCondition(CaseFileCategory.APPEALS))
                .mandatory(DocumentAmendDetails::getCorrespondenceDocuments,
                    documentsShowCondition(CaseFileCategory.CORRESPONDENCE))
                .label("correspondenceNoDocumentsQuestion", "Which document do you want to amend?",
                    noDocumentsShowCondition(CaseFileCategory.CORRESPONDENCE))
                .label("correspondenceNoDocuments", noDocumentsMessage(CaseFileCategory.CORRESPONDENCE),
                    noDocumentsShowCondition(CaseFileCategory.CORRESPONDENCE))
                .mandatory(DocumentAmendDetails::getUncategorisedDocuments,
                    documentsShowCondition(CaseFileCategory.UNCATEGORISED_DOCUMENTS))
                .label("uncategorisedDocumentsNoDocumentsQuestion", "Which document do you want to amend?",
                    noDocumentsShowCondition(CaseFileCategory.UNCATEGORISED_DOCUMENTS))
                .label("uncategorisedDocumentsNoDocuments",
                    noDocumentsMessage(CaseFileCategory.UNCATEGORISED_DOCUMENTS),
                    noDocumentsShowCondition(CaseFileCategory.UNCATEGORISED_DOCUMENTS))
                .readonly(DocumentAmendDetails::getStatementsOfCaseEmpty, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getPropertyDocumentsEmpty, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getEvidenceEmpty, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getHearingDocumentsEmpty, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getOrdersAndNoticeOfHearingsEmpty, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getApplicationsEmpty, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getAppealsEmpty, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getCorrespondenceEmpty, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getUncategorisedDocumentsEmpty, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getSelectedFolderId, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getSelectedFolderLabel, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getSelectedDocumentId, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getSelectedDocumentFileName, NEVER_SHOW, true)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        documentAmendSelectionService.initialise(details.getId(), caseData);
        List<String> errors = documentAmendSelectionService.validateAndStoreSelection(caseData);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }

    private String documentsShowCondition(CaseFileCategory category) {
        return selectedFolderCondition(category) + " AND " + emptyFieldId(category) + NO;
    }

    private String noDocumentsShowCondition(CaseFileCategory category) {
        return selectedFolderCondition(category) + " AND " + emptyFieldId(category) + YES;
    }

    private String selectedFolderCondition(CaseFileCategory category) {
        return FIELD_PREFIX + "SelectedFolder=\"" + folderForCategory(category).name() + "\"";
    }

    private DocumentAmendFolder folderForCategory(CaseFileCategory category) {
        return switch (category) {
            case STATEMENTS_OF_CASE -> DocumentAmendFolder.STATEMENTS_OF_CASE;
            case PROPERTY_DOCUMENTS -> DocumentAmendFolder.PROPERTY_DOCUMENTS;
            case EVIDENCE -> DocumentAmendFolder.EVIDENCE;
            case HEARING_DOCUMENTS -> DocumentAmendFolder.HEARING_DOCUMENTS;
            case ORDERS_AND_NOTICE_OF_HEARINGS -> DocumentAmendFolder.ORDERS_AND_NOTICE_OF_HEARINGS;
            case APPLICATIONS -> DocumentAmendFolder.APPLICATIONS;
            case APPEALS -> DocumentAmendFolder.APPEALS;
            case CORRESPONDENCE -> DocumentAmendFolder.CORRESPONDENCE;
            case UNCATEGORISED_DOCUMENTS -> DocumentAmendFolder.UNCATEGORISED_DOCUMENTS;
        };
    }

    private String emptyFieldId(CaseFileCategory category) {
        return switch (category) {
            case STATEMENTS_OF_CASE -> FIELD_PREFIX + "StatementsOfCaseEmpty";
            case PROPERTY_DOCUMENTS -> FIELD_PREFIX + "PropertyDocumentsEmpty";
            case EVIDENCE -> FIELD_PREFIX + "EvidenceEmpty";
            case HEARING_DOCUMENTS -> FIELD_PREFIX + "HearingDocumentsEmpty";
            case ORDERS_AND_NOTICE_OF_HEARINGS -> FIELD_PREFIX + "OrdersAndNoticeOfHearingsEmpty";
            case APPLICATIONS -> FIELD_PREFIX + "ApplicationsEmpty";
            case APPEALS -> FIELD_PREFIX + "AppealsEmpty";
            case CORRESPONDENCE -> FIELD_PREFIX + "CorrespondenceEmpty";
            case UNCATEGORISED_DOCUMENTS -> FIELD_PREFIX + "UncategorisedDocumentsEmpty";
        };
    }

    private String noDocumentsMessage(CaseFileCategory category) {
        return "No documents in '" + category.getLabel() + "'";
    }
}
