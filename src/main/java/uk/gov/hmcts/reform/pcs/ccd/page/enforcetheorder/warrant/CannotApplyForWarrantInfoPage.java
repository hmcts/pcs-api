package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

public class CannotApplyForWarrantInfoPage implements CcdPageConfiguration {

    static final String ERROR_MESSAGE = "You cannot continue with this application until "
        + "you ask the judge for permission to apply for a warrant of possession";

    private static final String MARKDOWN_TEXT = """
            <p class="govuk-body">You need permission from a judge before you can apply for the warrant of possession.
             This is because the defendants have not missed any of the payments outlined in the suspended order.</p>
            <p class="govuk-body govuk-!-font-weight-bold">If the defendants have breached a
             different term in the suspended order</p>
            <p class="govuk-body">If the defendants have breached a different term in the order,
             you still need permission from a judge.  For example, if they have breached an order
              to reduce their antisocial behaviour.</p>
            <p><a id="cannotApplyForWarrantInfo-link" class="govuk-link" target="_blank" href="#">
        Ask the judge for permission to apply for a warrant of possession (opens in a new tab, GOV.UK).</a></p>
            """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("cannotApplyForWarrantInfo", this::midEvent)
            .pageLabel("You need permission from a judge to apply for the warrant of possession")
            .showCondition(
                ShowConditions.and(
                    ShowConditionsEnforcementType.WARRANT_FLOW,
                    "warrantDefendantMissedPayments=\"NO\""
                )
            )
            .label("cannotApplyForWarrantInfo-line-separator", "---")
            .label("cannotApplyForWarrantInfo-text", MARKDOWN_TEXT);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
            CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> before) {

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride(ERROR_MESSAGE)
            .build();
    }
}
