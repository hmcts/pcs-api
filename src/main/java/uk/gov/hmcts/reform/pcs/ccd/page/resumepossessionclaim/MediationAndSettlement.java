package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

/**
 * Page configuration for the Mediation and Settlement section.
 * Allows claimants to indicate whether they're willing to try mediation or settlement
 * with optional additional information fields that appear conditionally.
 */
@AllArgsConstructor
@Component
public class MediationAndSettlement implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("mediationAndSettlement", this::midEvent)
                .pageLabel("Mediation and settlement")
                .label("mediationAndSettlement-content",
                        """
                        ---
                        <section tabindex="0">
                            <p class="govuk-body">
                                Mediation is when an impartial professional (the mediator) helps both sides work out \
                                an agreement.
                            </p>
                        </section>
                        """)
                .mandatory(PCSCase::getMediationAttempted)
                .mandatory(PCSCase::getMediationAttemptedDetails, "mediationAttempted=\"YES\"")
                .label("settlement-section",
                        """
                        ---
                        <section tabindex="0">
                            <p class="govuk-body">
                                If your claim is on the grounds of rent arrears, this includes any steps you’ve taken \
                                to recover the arrears or to agree a repayment plan.
                            </p>
                        </section>
                        """)
                .mandatory(PCSCase::getSettlementAttempted)
                .mandatory(PCSCase::getSettlementAttemptedDetails, "settlementAttempted=\"YES\"")
                .label("mediationAndSettlement-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        // Validate text area fields for character limit - ultra simple approach
        List<String> validationErrors = textAreaValidationService.validateMultipleTextAreas(
            TextAreaValidationService.FieldValidation.of(
                caseData.getMediationAttemptedDetails(),
                "Give details about the attempted mediation and what the outcome was",
                TextAreaValidationService.SHORT_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                caseData.getSettlementAttemptedDetails(),
                "Explain what steps you’ve taken to reach a settlement",
                TextAreaValidationService.SHORT_TEXT_LIMIT
            )
        );

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
