package uk.gov.hmcts.reform.pcs.ccd.page.documentremoval;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.TypedPropertyGetter;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentremoval.DocumentRemovalDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendSelectionService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class SelectDocumentToRemovePage implements CcdPageConfiguration {

    private static final String PAGE_ID = "selectDocumentToRemove";
    private static final String FIELD_PREFIX = "documentRemoval_";
    private static final String YES = "=\"Yes\"";
    private static final String NO = "=\"No\"";
    private static final String DOCUMENT_QUESTION_LABEL = "Which document do you want to remove?";
    private static final String REASON_LABEL = "Why are you removing this document?";

    private final DocumentAmendSelectionService documentSelectionService;
    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        FieldCollectionBuilder<PCSCase, State, ?> page = pageBuilder
            .page(PAGE_ID, this::midEvent)
            .pageLabel("Select document")
            .label(PAGE_ID + "-separator", "---");

        page.complex(PCSCase::getDocumentRemovalDetails)
                .readonly(DocumentRemovalDetails::getPropertyAddressSummary, NEVER_SHOW, true)
                .mandatory(DocumentRemovalDetails::getSelectedFolder)
                .label("emptyFolderDocumentError", "", NEVER_SHOW)
                .label("selectedFolderEmptyErrorMessage", "", NEVER_SHOW)
                .label("emptyFolderDocumentQuestion", "", NEVER_SHOW)
            .done();

        for (DocumentCategoryField categoryField : DocumentCategoryField.values()) {
            addCategoryFields(page, categoryField);
        }

        FieldCollectionBuilder<DocumentRemovalDetails, State, ?> documentRemovalDetailsFields =
            page.complex(PCSCase::getDocumentRemovalDetails);

        for (DocumentCategoryField categoryField : DocumentCategoryField.values()) {
            documentRemovalDetailsFields.readonly(categoryField.emptyGetter, NEVER_SHOW, true);
        }

        for (DocumentCategoryField categoryField : DocumentCategoryField.values()) {
            documentRemovalDetailsFields.mandatoryWithoutDefaultValue(categoryField.reasonGetter,
                                                              reasonShowCondition(categoryField), REASON_LABEL, false);
        }

        documentRemovalDetailsFields
                .readonly(DocumentRemovalDetails::getSelectedFolderId, NEVER_SHOW, true)
                .readonly(DocumentRemovalDetails::getSelectedFolderLabel, NEVER_SHOW, true)
                .readonly(DocumentRemovalDetails::getSelectedDocumentId, NEVER_SHOW, true)
                .readonly(DocumentRemovalDetails::getSelectedDocumentFileName, NEVER_SHOW, true)
            .done();
    }

    private void addCategoryFields(FieldCollectionBuilder<PCSCase, State, ?> page,
                                   DocumentCategoryField categoryField) {
        page
            .label(categoryField.documentsFieldId + "EmptyFolderMessage",
                   emptyFolderMessage(categoryField.category), noDocumentsShowCondition(categoryField))
            .mandatoryWithoutDefaultValue(categoryField.documentsGetter, documentsShowCondition(categoryField),
                                          DOCUMENT_QUESTION_LABEL, false);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        if (caseData.getDocumentRemovalDetails() == null) {
            caseData.setDocumentRemovalDetails(new DocumentRemovalDetails());
        }
        DocumentRemovalDetails documentRemovalDetails = caseData.getDocumentRemovalDetails();

        documentSelectionService.initialise(details.getId(), caseData, documentRemovalDetails);
        List<String> errors = new ArrayList<>(
            documentSelectionService.validateAndStoreSelection(caseData, documentRemovalDetails));

        CaseFileCategory selectedFolder = documentRemovalDetails.getSelectedFolder();
        if (selectedFolder != null) {
            String reason = documentRemovalDetails.getReasonForCategory(selectedFolder);
            errors.addAll(textAreaValidationService.validateSingleTextArea(
                reason, REASON_LABEL, TextAreaValidationService.SHORT_TEXT_LIMIT));
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
            .build();
    }

    private String documentsShowCondition(DocumentCategoryField categoryField) {
        return selectedFolderCondition(categoryField.category) + " AND " + FIELD_PREFIX
            + categoryField.emptyFieldId + NO;
    }

    private String noDocumentsShowCondition(DocumentCategoryField categoryField) {
        return selectedFolderCondition(categoryField.category) + " AND " + FIELD_PREFIX
            + categoryField.emptyFieldId + YES;
    }

    private String reasonShowCondition(DocumentCategoryField categoryField) {
        return selectedFolderCondition(categoryField.category) + " AND "
            + sharedDocumentsFieldId(categoryField.category) + "!=\"\"";
    }

    private String sharedDocumentsFieldId(CaseFileCategory category) {
        return switch (category) {
            case STATEMENTS_OF_CASE -> "statementsOfCaseDocuments";
            case PROPERTY_DOCUMENTS -> "propertyDocuments";
            case EVIDENCE -> "evidenceDocuments";
            case HEARING_DOCUMENTS -> "hearingDocuments";
            case ORDERS_AND_NOTICE_OF_HEARINGS -> "ordersAndNoticeOfHearingsDocuments";
            case APPLICATIONS -> "applicationsDocuments";
            case APPEALS -> "appealsDocuments";
            case CORRESPONDENCE -> "correspondenceDocuments";
            case UNCATEGORISED_DOCUMENTS -> "uncategorisedDocuments";
        };
    }

    private String selectedFolderCondition(CaseFileCategory category) {
        return FIELD_PREFIX + "SelectedFolder=\"" + category.name() + "\"";
    }

    private TypedPropertyGetter<DocumentRemovalDetails, String> reasonGetter(CaseFileCategory category) {
        for (DocumentCategoryField categoryField : DocumentCategoryField.values()) {
            if (categoryField.category == category) {
                return categoryField.reasonGetter;
            }
        }
        throw new IllegalStateException("No reason field configured for category " + category);
    }

    private String emptyFolderMessage(CaseFileCategory category) {
        return DOCUMENT_QUESTION_LABEL
            + "<br>"
            + "<span class=\"govuk-!-font-size-16\">No documents in '" + category.getLabel() + "'</span>";
    }

}
