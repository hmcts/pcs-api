package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class CompletingYourClaim implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("completingYourClaim", this::midEvent)
            .pageLabel("Completing your claim")


            // ---------- Horizontal separator ----------
            .label("completingYourClaim-separator", "---")
            .label(
                "completingYourClaim-intro",
                """
                <p class="govuk-body">
                  There are two options for what to do next:
                </p>
                <ul class="govuk-list govuk-list--bullet">
                  <li class="govuk-!-font-size-19">
                    sign the statement of truth, check your answers, then
                    submit and pay for your claim now.
                  </li>
                  <li class="govuk-!-font-size-19">
                    check your answers and save your claim as a draft.
                    You can return later to sign the statement of truth
                    and submit and pay.
                  </li>
                </ul>
                """
            )
            .mandatory(PCSCase::getCompletionNextStep)
            .label("completingYourClaim-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        caseData.setSaveButtonLabelOnCaseSubmit(
            CompletionNextStep.SAVE_IT_FOR_LATER == caseData.getCompletionNextStep()
                ? "Save claim"
                : "Submit claim"
        );
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
