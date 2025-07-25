package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class ClaimantTypeNotEligibleWales implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantTypeNotEligibleWales", this::midEvent)
            .pageLabel("You're not eligible for this online service")
            .showCondition("showClaimantTypeNotEligibleWales=\"Yes\"")
            .readonly(PCSCase::getShowClaimantTypeNotEligibleWales, NEVER_SHOW)
            .label("claimantTypeNotEligibleWales-info", """
                ---
                <p class="govuk-body">
                This service is currently only available for registered community landlords.

                <h3 class="govuk-heading-s govuk-!-font-size-19">What to do next</h3>

                Use form N5W and the correct particulars of claim form.

                <a href="https://www.gov.uk/government/collections/property-possession-forms" rel="noreferrer noopener"
                    target="_blank"
                    class="govuk-link">View the full list of property possessions forms (opens in a new tab)</a>.
                </p>

                <a href="/cases" role="button" draggable="false" class="govuk-button" data-module="govuk-button">
                  Close and return to case list
                </a>
                """);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errors(List.of("You're not eligible for this online service"))
            .build();
    }

}
