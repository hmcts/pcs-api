package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.ArrayList;
import java.util.List;

@Component
public class UploadAdditionalDocumentsDetails implements CcdPageConfiguration {

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
                       You must select the type of document you're uploading and give it a short description.
                       </p>
                       """
                )
            .label("uploadAdditionalDocuments-heading",
                   """
                   <h2>Before you upload your documents</h2>
                   <p class="govuk-body govuk-!-font-size-19">Give your document a name that explains what it is.</p>
                   """
            )

            .mandatory(PCSCase::getAdditionalDocuments);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();

        if (caseData.getAdditionalDocuments() != null) {
            caseData.getAdditionalDocuments().stream()
                .map(ListValue::getValue)
                .filter(document -> document != null && document.getDescription() != null)
                .forEach(document -> {
                    String description = document.getDescription();
                    if (description.length() > 62) {
                        errors.add("The explanation must be 62 characters or fewer");
                    }
                });
        }

        if (errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
        } else {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errors(errors)
                .build();
        }
    }
}
