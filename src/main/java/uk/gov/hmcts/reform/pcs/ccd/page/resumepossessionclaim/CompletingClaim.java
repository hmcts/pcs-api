package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class CompletingClaim implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("completingClaim")
            .pageLabel("Completing your claim")

            // ---------- Horizontal separator ----------
            .label("completing-your-claim-separator", "---")
            .label(
                "completing-your-claim-intro",
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
            .mandatory(PCSCase::getCompletionNextStep);
    }
}
