package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadAdditionalDocumentsDetails implements CcdPageConfiguration {

    // Error message constants
    private static final String DESCRIPTION_REQUIRED_ERROR =
        "Short description is required";
    private static final String DESCRIPTION_TOO_SHORT_ERROR =
        "Short description must be at least 5 characters long";

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

    /**
     * Mid-event validation for additional documents.
     * Validates that short descriptions are at least 5 characters long.
     */
    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        log.info("UploadAdditionalDocumentsDetails midEvent called");
        PCSCase caseData = details.getData();

        List<String> validationErrors = validateAdditionalDocuments(caseData);
        log.info("Validation errors found: {}", validationErrors);

        if (!validationErrors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errors(validationErrors)
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    /**
     * Validates additional documents and returns any validation errors.
     * @param caseData the case data containing additional documents
     * @return list of error messages, empty if no errors
     */
    private List<String> validateAdditionalDocuments(PCSCase caseData) {
        List<String> errors = new ArrayList<>();
        log.info("Validating additional documents, count: {}", 
                caseData.getAdditionalDocuments() != null ? caseData.getAdditionalDocuments().size() : 0);

        if (caseData.getAdditionalDocuments() != null) {
            for (ListValue<AdditionalDocument> doc : caseData.getAdditionalDocuments()) {
                if (doc.getValue() != null) {
                    log.info("Validating document description: '{}'", doc.getValue().getDescription());
                    validateDescription(doc.getValue().getDescription(), errors);
                }
            }
        }

        return errors;
    }

    /**
     * Validates a description field with mandatory and minimum length validation.
     * @param description the description to validate
     * @param errors the list to add errors to
     */
    private void validateDescription(String description, List<String> errors) {
        if (description == null || description.trim().isEmpty()) {
            errors.add(DESCRIPTION_REQUIRED_ERROR);
        } else if (description.trim().length() < 5) {
            errors.add(DESCRIPTION_TOO_SHORT_ERROR);
        }
    }
}
