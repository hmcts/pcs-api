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
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendSelectionService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class SelectDocumentPage implements CcdPageConfiguration {

    private static final String PAGE_ID = "selectDocument";
    private static final String FIELD_PREFIX = "documentAmend_";
    private static final String YES = "=\"Yes\"";
    private static final String NO = "=\"No\"";
    private static final String SELECT_DIFFERENT_FOLDER_ERROR = "Select a different folder to continue";
    private final DocumentAmendSelectionService documentAmendSelectionService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(PAGE_ID, this::midEvent)
            .pageLabel("Select document")
            .label(PAGE_ID + "-separator", "---")
            .complex(PCSCase::getDocumentAmendDetails)
                .readonly(DocumentAmendDetails::getPropertyAddressSummary, NEVER_SHOW, true)
                .mandatory(DocumentAmendDetails::getSelectedFolder)
                .label("emptyFolderDocumentError", "", NEVER_SHOW)
                .label("selectedFolderEmptyErrorMessage", "", NEVER_SHOW)
                .label("emptyFolderDocumentQuestion", "", NEVER_SHOW)
                .label("statementsOfCaseEmptyFolderError", errorMessage(SELECT_DIFFERENT_FOLDER_ERROR),
                    emptyFolderErrorShowCondition(CaseFileCategory.STATEMENTS_OF_CASE))
                .label("statementsOfCaseEmptyFolderQuestion", documentQuestion(),
                    noDocumentsShowCondition(CaseFileCategory.STATEMENTS_OF_CASE))
                .label("propertyDocumentsEmptyFolderError", errorMessage(SELECT_DIFFERENT_FOLDER_ERROR),
                    emptyFolderErrorShowCondition(CaseFileCategory.PROPERTY_DOCUMENTS))
                .label("propertyDocumentsEmptyFolderQuestion", documentQuestion(),
                    noDocumentsShowCondition(CaseFileCategory.PROPERTY_DOCUMENTS))
                .label("evidenceEmptyFolderError", errorMessage(SELECT_DIFFERENT_FOLDER_ERROR),
                    emptyFolderErrorShowCondition(CaseFileCategory.EVIDENCE))
                .label("evidenceEmptyFolderQuestion", documentQuestion(),
                    noDocumentsShowCondition(CaseFileCategory.EVIDENCE))
                .label("hearingDocumentsEmptyFolderError", errorMessage(SELECT_DIFFERENT_FOLDER_ERROR),
                    emptyFolderErrorShowCondition(CaseFileCategory.HEARING_DOCUMENTS))
                .label("hearingDocumentsEmptyFolderQuestion", documentQuestion(),
                    noDocumentsShowCondition(CaseFileCategory.HEARING_DOCUMENTS))
                .label("ordersAndNoticeOfHearingsEmptyFolderError", errorMessage(SELECT_DIFFERENT_FOLDER_ERROR),
                    emptyFolderErrorShowCondition(CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS))
                .label("ordersAndNoticeOfHearingsEmptyFolderQuestion", documentQuestion(),
                    noDocumentsShowCondition(CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS))
                .label("applicationsEmptyFolderError", errorMessage(SELECT_DIFFERENT_FOLDER_ERROR),
                    emptyFolderErrorShowCondition(CaseFileCategory.APPLICATIONS))
                .label("applicationsEmptyFolderQuestion", documentQuestion(),
                    noDocumentsShowCondition(CaseFileCategory.APPLICATIONS))
                .label("appealsEmptyFolderError", errorMessage(SELECT_DIFFERENT_FOLDER_ERROR),
                    emptyFolderErrorShowCondition(CaseFileCategory.APPEALS))
                .label("appealsEmptyFolderQuestion", documentQuestion(),
                    noDocumentsShowCondition(CaseFileCategory.APPEALS))
                .label("correspondenceEmptyFolderError", errorMessage(SELECT_DIFFERENT_FOLDER_ERROR),
                    emptyFolderErrorShowCondition(CaseFileCategory.CORRESPONDENCE))
                .label("correspondenceEmptyFolderQuestion", documentQuestion(),
                    noDocumentsShowCondition(CaseFileCategory.CORRESPONDENCE))
                .label("uncategorisedDocumentsEmptyFolderError", errorMessage(SELECT_DIFFERENT_FOLDER_ERROR),
                    emptyFolderErrorShowCondition(CaseFileCategory.UNCATEGORISED_DOCUMENTS))
                .label("uncategorisedDocumentsEmptyFolderQuestion", documentQuestion(),
                    noDocumentsShowCondition(CaseFileCategory.UNCATEGORISED_DOCUMENTS))
                .mandatory(DocumentAmendDetails::getStatementsOfCaseDocuments,
                    documentsShowCondition(CaseFileCategory.STATEMENTS_OF_CASE), true)
                .label("statementsOfCaseNoDocuments", noDocumentsMessage(CaseFileCategory.STATEMENTS_OF_CASE),
                    noDocumentsShowCondition(CaseFileCategory.STATEMENTS_OF_CASE))
                .mandatory(DocumentAmendDetails::getPropertyDocuments,
                    documentsShowCondition(CaseFileCategory.PROPERTY_DOCUMENTS), true)
                .label("propertyDocumentsNoDocuments", noDocumentsMessage(CaseFileCategory.PROPERTY_DOCUMENTS),
                    noDocumentsShowCondition(CaseFileCategory.PROPERTY_DOCUMENTS))
                .mandatory(DocumentAmendDetails::getEvidenceDocuments,
                    documentsShowCondition(CaseFileCategory.EVIDENCE), true)
                .label("evidenceNoDocuments", noDocumentsMessage(CaseFileCategory.EVIDENCE),
                    noDocumentsShowCondition(CaseFileCategory.EVIDENCE))
                .mandatory(DocumentAmendDetails::getHearingDocuments,
                    documentsShowCondition(CaseFileCategory.HEARING_DOCUMENTS), true)
                .label("hearingDocumentsNoDocuments", noDocumentsMessage(CaseFileCategory.HEARING_DOCUMENTS),
                    noDocumentsShowCondition(CaseFileCategory.HEARING_DOCUMENTS))
                .mandatory(DocumentAmendDetails::getOrdersAndNoticeOfHearingsDocuments,
                    documentsShowCondition(CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS), true)
                .label("ordersAndNoticeOfHearingsNoDocuments",
                    noDocumentsMessage(CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS),
                    noDocumentsShowCondition(CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS))
                .mandatory(DocumentAmendDetails::getApplicationsDocuments,
                    documentsShowCondition(CaseFileCategory.APPLICATIONS), true)
                .label("applicationsNoDocuments", noDocumentsMessage(CaseFileCategory.APPLICATIONS),
                    noDocumentsShowCondition(CaseFileCategory.APPLICATIONS))
                .mandatory(DocumentAmendDetails::getAppealsDocuments,
                    documentsShowCondition(CaseFileCategory.APPEALS), true)
                .label("appealsNoDocuments", noDocumentsMessage(CaseFileCategory.APPEALS),
                    noDocumentsShowCondition(CaseFileCategory.APPEALS))
                .mandatory(DocumentAmendDetails::getCorrespondenceDocuments,
                    documentsShowCondition(CaseFileCategory.CORRESPONDENCE), true)
                .label("correspondenceNoDocuments", noDocumentsMessage(CaseFileCategory.CORRESPONDENCE),
                    noDocumentsShowCondition(CaseFileCategory.CORRESPONDENCE))
                .mandatory(DocumentAmendDetails::getUncategorisedDocuments,
                    documentsShowCondition(CaseFileCategory.UNCATEGORISED_DOCUMENTS), true)
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
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
            .build();
    }

    private String documentsShowCondition(CaseFileCategory category) {
        return selectedFolderCondition(category) + " AND " + emptyFieldId(category) + NO;
    }

    private String noDocumentsShowCondition(CaseFileCategory category) {
        return selectedFolderCondition(category) + " AND " + emptyFieldId(category) + YES;
    }

    private String emptyFolderErrorShowCondition(CaseFileCategory category) {
        return noDocumentsShowCondition(category);
    }

    private String selectedFolderCondition(CaseFileCategory category) {
        return FIELD_PREFIX + "SelectedFolder=\"" + category.name() + "\"";
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

    private String documentQuestion() {
        return "Which document do you want to amend?";
    }

    private String errorMessage(String message) {
        return "<p class=\"govuk-error-message\">" + message + "</p>";
    }
}
