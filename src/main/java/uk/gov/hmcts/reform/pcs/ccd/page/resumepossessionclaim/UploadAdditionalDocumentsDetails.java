package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
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

    /**
     * Mid-event handler to validate additional document descriptions.
     * Checks that short descriptions do not exceed 62 characters.
     */
    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                CaseDetails<PCSCase, State> detailsBefore) {
        log.info("Starting mid-event validation for UploadAdditionalDocumentsDetails");
        
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();

        if (caseData.getAdditionalDocuments() == null) {
            log.info("No additional documents found - validation passed");
        } else {
            int documentCount = caseData.getAdditionalDocuments().size();
            log.info("Found {} additional documents to validate", documentCount);
            
            caseData.getAdditionalDocuments().stream()
                .map(ListValue::getValue)
                .filter(document -> document != null && document.getDescription() != null)
                .forEach(document -> {
                    String description = document.getDescription();
                    int descriptionLength = description.length();
                    
                    log.debug("Validating document description: length={}, content='{}'", 
                             descriptionLength, description);
                    
                    if (descriptionLength > 62) {
                        String errorMessage = "The explanation must be 62 characters or fewer";
                        log.warn("Document description exceeds 62 characters: length={}, content='{}'", 
                                descriptionLength, description);
                        errors.add(errorMessage);
                    } else {
                        log.debug("Document description validation passed: length={}", descriptionLength);
                    }
                });
        }

        if (errors.isEmpty()) {
            log.info("Mid-event validation completed successfully - no errors found");
        } else {
            log.warn("Mid-event validation completed with {} errors: {}", errors.size(), errors);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errors(errors)
            .build();
    }
}
