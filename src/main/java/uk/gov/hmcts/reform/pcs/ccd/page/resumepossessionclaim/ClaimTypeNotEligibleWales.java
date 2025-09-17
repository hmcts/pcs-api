package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class ClaimTypeNotEligibleWales implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimTypeNotEligibleWales", this::midEvent)
            .pageLabel("You're not eligible for this online service")
            .showCondition("showClaimTypeNotEligibleWales=\"Yes\"")
            .readonly(PCSCase::getShowClaimTypeNotEligibleWales, NEVER_SHOW)
            .label("claimTypeNotEligibleWales-info", """
                ---
                <p class="govuk-body">
                You cannot make a trespass claim using this service.

                <h3 class="govuk-heading-s govuk-!-font-size-19">What to do next</h3>

                Use form N5 Wales and the correct particulars of claim form.

                <a href="https://www.gov.uk/government/collections/property-possession-forms" rel="noreferrer noopener"
                    target="_blank"
                    class="govuk-link">View the full list of property possessions forms (opens in a new tab)</a>.
                </p>

                <div class="govuk-warning-text" role="alert" aria-labelledby="warning-message">
                  <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                  <strong class="govuk-warning-text__text">
                    <span class="govuk-warning-text__assistive">Warning</span>
                    <span id="warning-message">
                      To exit back to the case list, select 'Cancel'
                    </span>
                  </strong>
                </div>
                """);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errors(List.of("You're not eligible for this online service"))
            .build();
    }

}
