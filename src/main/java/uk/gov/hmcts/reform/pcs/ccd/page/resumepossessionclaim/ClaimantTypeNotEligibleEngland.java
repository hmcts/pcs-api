package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class ClaimantTypeNotEligibleEngland implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantTypeNotEligibleEngland", this::midEvent)
            .pageLabel("You're not eligible for this online service")
            .showCondition("showClaimantTypeNotEligibleEngland=\"Yes\"")
            .readonly(PCSCase::getShowClaimantTypeNotEligibleEngland, NEVER_SHOW)
            .label("claimantTypeNotEligibleEngland-info", """
                ---
                <p class="govuk-body">
                This service is currently only available for registered providers of social housing.

                <h3 class="govuk-heading-s govuk-!-font-size-19">What to do next</h3>

                <ul class="govuk-list">
                  <li class="govuk-!-font-size-19">
                    <span class="govuk-!-font-weight-bold">For rental or mortgage arrears claims</span> – use the
                    <a href="https://www.gov.uk/possession-claim-online-recover-property"
                        target="_blank"
                        class="govuk-link">Possession Claim Online (PCOL) service (opens in a new tab)</a>.
                  </li>
                  <li class="govuk-!-font-size-19">
                    <span class="govuk-!-font-weight-bold">For other types of claims</span> – use form N5
                    and the correct particulars of claim form.
                  </li>
                </ul>

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
