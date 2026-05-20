package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class MustApplyForHelpWithFees implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">
            The defendant needs to <a href="https://www.gov.uk/get-help-with-court-fees" target="_blank"
            rel="noopener noreferrer" class="govuk-link">apply for Help with Fees (GOV.UK, opens in new tab)</a>
            before you can continue with this application. If you are applying for Help with Fees on the defendant’s
            behalf, you should do that now before you continue with this application.
        </p>
        <p class="govuk-body">
            Enter the court form number 'N244' when asked. This will be one of the first questions
            when you (or they) apply for Help with Fees.
        </p>
        <p class="govuk-body">
            After you (or they) have applied you (or they) will receive a Help With Fees reference number.
            Enter the reference number when you return to this application.
        </p>
        <p class="govuk-body">
            If they receive any benefit that qualifies them for Help with Fees, you (or they) must include
            evidence of it when you (or they) apply online for Help with Fees.
        </p>
        <p class="govuk-body">
            If you already have their Help with Fees reference number, you can return to the previous screen
            and enter it there.
        </p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("mustApplyForHelpWithFees", this::midEvent)
            .pageLabel("The defendant needs to apply for help with their application fee")
            .showCondition(ShowConditions.and(
                fieldEquals("xui_genapp_ShowHwfScreens", VerticalYesNo.YES),
                fieldEquals("xui_genapp_NeedHwf", VerticalYesNo.YES),
                fieldEquals("xui_genapp_AppliedForHwf", VerticalYesNo.NO)
            ))
            .label("mustApplyForHelpWithFees-lineSeparator", "---")
            .label("mustApplyForHelpWithFees-info", INFO_MARKDOWN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride("You cannot continue until you have their reference number for Help with Fees")
            .build();
    }

}
