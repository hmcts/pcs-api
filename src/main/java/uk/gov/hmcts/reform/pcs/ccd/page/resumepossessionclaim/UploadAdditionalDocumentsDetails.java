package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class UploadAdditionalDocumentsDetails implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;
    private static final String DESCRIPTION_LABEL = "short description";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadAdditionalDocuments", this::midEvent)
            .pageLabel("Upload additional documents")
            .showCondition("wantToUploadDocuments=\"YES\"")

            // ---------- Horizontal separator ----------
            .label("uploadAdditionalDocuments-separator", "---")
                .label("uploadAdditionalDocuments-separator-help",
                       """
                       <p class="govuk-body govuk-!-font-size-19">
                       You must select the type of document you’re uploading and give it a short description.
                       </p>
                       """
                )
            .label("uploadAdditionalDocuments-heading",
                   """
                   <h2>Before you upload your documents</h2>
                   <p class="govuk-body govuk-!-font-size-19">Give your document a name that explains what it is.</p>
                   """
            )

            .mandatory(PCSCase::getAdditionalDocuments)
            .label("uploadAdditionalDocuments-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> errors = validateDocumentDescription(caseData.getAdditionalDocuments(), DESCRIPTION_LABEL);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
            .data(caseData)
            .build();
    }

    public List<String> validateDocumentDescription(
        List<ListValue<AdditionalDocument>> additionalDocs,
        String sectionLabel) {

        List<String> validationErrors = new ArrayList<>();

        for (int i = 0; i < additionalDocs.size(); i++) {
            String docDescription = additionalDocs.get(i).getValue().getDescription();
            String sectionHint = "Additional document %d".formatted(i + 1) + "'s " + sectionLabel;
            validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                docDescription, sectionHint, TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT)
            );
        }
        return validationErrors;
    }

}
