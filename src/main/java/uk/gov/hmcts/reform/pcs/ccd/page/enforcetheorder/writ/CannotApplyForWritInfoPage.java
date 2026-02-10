package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

import java.util.List;

public class CannotApplyForWritInfoPage implements CcdPageConfiguration {

    static final String ERROR_MESSAGE =
            "You cannot continue with this application until you ask the judge for permission "
                    + "to transfer to the High Court";

    private static final String MARKDOWN_TEXT = """
            <p class="govuk-body">You must transfer your claim from the County Court to the High Court before you can
            apply for a writ. You need permission from the court to do this.</p>
            <p class="govuk-body">Transferring to the High Court means that a High Court Enforcement Officer can carry 
            out the eviction.</p>
            <p class="govuk-body">Once you have permission to transfer to the High Court, you 
            can return to this service and apply for a writ.</p>
            <p class="govuk-body govuk-!-font-weight-bold">How to transfer your claim to 
            the High Court</p>
            <p class="govuk-body">You must make an application if you want to transfer your claim to the High Court.</p>
            <p><a id="cannotApplyForWritInfo-link" class="govuk-link">
            Transfer your claim from the County Court to the High Court</a>
            (make an application)</p>
            """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("cannotApplyForWritInfo", this::midEvent)
            .pageLabel("You cannot apply for a writ until you have transferred your claim to the High Court")
            .showCondition(ShowConditionsWarrantOrWrit.WRIT_FLOW
                    + " AND writHasClaimTransferredToHighCourt=\"No\"")
            .label("cannotApplyForWritInfo-line-separator", "---")
            .label("cannotApplyForWritInfo-text", MARKDOWN_TEXT);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
            CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> before) {

        // Always return an error to block progression
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of(ERROR_MESSAGE))
                .build();
    }
}
