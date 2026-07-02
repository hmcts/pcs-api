package uk.gov.hmcts.reform.pcs.ccd.page.documentamend;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.TypedPropertyGetter;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
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
import static uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendSelectionService.SELECT_DIFFERENT_FOLDER_ERROR;

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
        FieldCollectionBuilder<DocumentAmendDetails, State, ?> documentAmendPage = pageBuilder
            .page(PAGE_ID, this::midEvent)
            .pageLabel("Select document")
            .label(PAGE_ID + "-separator", "---")
            .complex(PCSCase::getDocumentAmendDetails)
                .readonly(DocumentAmendDetails::getPropertyAddressSummary, NEVER_SHOW, true)
                .mandatory(DocumentAmendDetails::getSelectedFolder)
                .label("emptyFolderDocumentError", "", NEVER_SHOW)
                .label("selectedFolderEmptyErrorMessage", "", NEVER_SHOW)
                .label("emptyFolderDocumentQuestion", "", NEVER_SHOW);

        for (DocumentCategoryField categoryField : DocumentCategoryField.values()) {
            addCategoryFields(documentAmendPage, categoryField);
        }

        for (DocumentCategoryField categoryField : DocumentCategoryField.values()) {
            documentAmendPage.readonly(categoryField.emptyGetter, NEVER_SHOW, true);
        }

        documentAmendPage
                .readonly(DocumentAmendDetails::getSelectedFolderId, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getSelectedFolderLabel, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getSelectedDocumentId, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getSelectedDocumentFileName, NEVER_SHOW, true)
            .done();
    }

    private void addCategoryFields(FieldCollectionBuilder<DocumentAmendDetails, State, ?> page,
                                   DocumentCategoryField categoryField) {
        CaseFileCategory category = categoryField.category;
        page
            .label(categoryField.idPrefix + "EmptyFolderError", errorMessage(SELECT_DIFFERENT_FOLDER_ERROR),
                   noDocumentsShowCondition(category))
            .label(categoryField.idPrefix + "EmptyFolderQuestion", documentQuestion(),
                   noDocumentsShowCondition(category))
            .mandatory(categoryField.documentsGetter, documentsShowCondition(category), true)
            .label(categoryField.idPrefix + "NoDocuments", noDocumentsMessage(category),
                   noDocumentsShowCondition(category));
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

    private enum DocumentCategoryField {
        STATEMENTS_OF_CASE(
            CaseFileCategory.STATEMENTS_OF_CASE,
            "statementsOfCase",
            DocumentAmendDetails::getStatementsOfCaseDocuments,
            DocumentAmendDetails::getStatementsOfCaseEmpty
        ),
        PROPERTY_DOCUMENTS(
            CaseFileCategory.PROPERTY_DOCUMENTS,
            "propertyDocuments",
            DocumentAmendDetails::getPropertyDocuments,
            DocumentAmendDetails::getPropertyDocumentsEmpty
        ),
        EVIDENCE(
            CaseFileCategory.EVIDENCE,
            "evidence",
            DocumentAmendDetails::getEvidenceDocuments,
            DocumentAmendDetails::getEvidenceEmpty
        ),
        HEARING_DOCUMENTS(
            CaseFileCategory.HEARING_DOCUMENTS,
            "hearingDocuments",
            DocumentAmendDetails::getHearingDocuments,
            DocumentAmendDetails::getHearingDocumentsEmpty
        ),
        ORDERS_AND_NOTICE_OF_HEARINGS(
            CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS,
            "ordersAndNoticeOfHearings",
            DocumentAmendDetails::getOrdersAndNoticeOfHearingsDocuments,
            DocumentAmendDetails::getOrdersAndNoticeOfHearingsEmpty
        ),
        APPLICATIONS(
            CaseFileCategory.APPLICATIONS,
            "applications",
            DocumentAmendDetails::getApplicationsDocuments,
            DocumentAmendDetails::getApplicationsEmpty
        ),
        APPEALS(
            CaseFileCategory.APPEALS,
            "appeals",
            DocumentAmendDetails::getAppealsDocuments,
            DocumentAmendDetails::getAppealsEmpty
        ),
        CORRESPONDENCE(
            CaseFileCategory.CORRESPONDENCE,
            "correspondence",
            DocumentAmendDetails::getCorrespondenceDocuments,
            DocumentAmendDetails::getCorrespondenceEmpty
        ),
        UNCATEGORISED_DOCUMENTS(
            CaseFileCategory.UNCATEGORISED_DOCUMENTS,
            "uncategorisedDocuments",
            DocumentAmendDetails::getUncategorisedDocuments,
            DocumentAmendDetails::getUncategorisedDocumentsEmpty
        );

        private final CaseFileCategory category;
        private final String idPrefix;
        private final TypedPropertyGetter<DocumentAmendDetails, DynamicList> documentsGetter;
        private final TypedPropertyGetter<DocumentAmendDetails, YesOrNo> emptyGetter;

        DocumentCategoryField(CaseFileCategory category,
                              String idPrefix,
                              TypedPropertyGetter<DocumentAmendDetails, DynamicList> documentsGetter,
                              TypedPropertyGetter<DocumentAmendDetails, YesOrNo> emptyGetter) {
            this.category = category;
            this.idPrefix = idPrefix;
            this.documentsGetter = documentsGetter;
            this.emptyGetter = emptyGetter;
        }
    }
}
