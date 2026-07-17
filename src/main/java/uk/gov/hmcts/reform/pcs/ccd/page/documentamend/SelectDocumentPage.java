package uk.gov.hmcts.reform.pcs.ccd.page.documentamend;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
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
    private static final String DOCUMENT_QUESTION_LABEL = "Which document do you want to amend?";
    private static final String YES = "=\"Yes\"";
    private static final String NO = "=\"No\"";
    private final DocumentAmendSelectionService documentSelectionService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        FieldCollectionBuilder<PCSCase, State, ?> page = pageBuilder
            .page(PAGE_ID, this::midEvent)
            .pageLabel("Select document")
            .label(PAGE_ID + "-separator", "---")
            .complex(PCSCase::getDocumentAmendDetails)
                .readonly(DocumentAmendDetails::getPropertyAddressSummary, NEVER_SHOW, true)
                .mandatory(DocumentAmendDetails::getSelectedFolder)
                .label("emptyFolderDocumentError", "", NEVER_SHOW)
                .label("selectedFolderEmptyErrorMessage", "", NEVER_SHOW)
                .label("emptyFolderDocumentQuestion", "", NEVER_SHOW)
            .done();

        for (DocumentAmendCategoryField categoryField : DocumentAmendCategoryField.values()) {
            addCategoryFields(page, categoryField);
        }

        FieldCollectionBuilder<DocumentAmendDetails, State, ?> documentAmendDetailsFields =
            page.complex(PCSCase::getDocumentAmendDetails);

        for (DocumentAmendCategoryField categoryField : DocumentAmendCategoryField.values()) {
            documentAmendDetailsFields.readonly(categoryField.emptyGetter, NEVER_SHOW, true);
        }

        documentAmendDetailsFields
                .readonly(DocumentAmendDetails::getSelectedFolderId, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getSelectedFolderLabel, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getSelectedDocumentId, NEVER_SHOW, true)
                .readonly(DocumentAmendDetails::getSelectedDocumentFileName, NEVER_SHOW, true)
            .done();
    }

    private void addCategoryFields(FieldCollectionBuilder<PCSCase, State, ?> page,
                                   DocumentAmendCategoryField categoryField) {
        CaseFileCategory category = categoryField.category;
        page
            .label(categoryField.idPrefix + "EmptyFolderMessage", emptyFolderMessage(category),
                   noDocumentsShowCondition(category))
            .mandatoryWithoutDefaultValue(categoryField.documentsGetter, documentsShowCondition(category),
                                          DOCUMENT_QUESTION_LABEL, true);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        if (caseData.getDocumentAmendDetails() == null) {
            caseData.setDocumentAmendDetails(new DocumentAmendDetails());
        }
        DocumentAmendDetails documentAmendDetails = caseData.getDocumentAmendDetails();

        documentSelectionService.initialise(details.getId(), caseData, documentAmendDetails);
        List<String> errors = documentSelectionService.validateAndStoreSelection(caseData, documentAmendDetails);

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

    private String emptyFolderMessage(CaseFileCategory category) {
        return DOCUMENT_QUESTION_LABEL
            + "<br>"
            + "<span class=\"govuk-!-font-size-16\">No documents in '" + category.getLabel() + "'</span>";
    }

}
