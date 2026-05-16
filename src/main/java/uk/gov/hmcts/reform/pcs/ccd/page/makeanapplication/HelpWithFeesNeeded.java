package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class HelpWithFeesNeeded implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">It usually costs ${xui_genapp_StandardFee} to apply. The fee will
        increase to ${xui_genapp_MaxFee} if:</p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">the defendant has already told the other party that they are making
          this application, and</li>
          <li class="govuk-!-font-size-19">the other party did not agree to it
          (this means that they objected to it)</li>
        </ul>
        <p class="govuk-body">You’ll see the final application fee before you pay.</p>

        <p class="govuk-body">The defendant may be able to get help paying the fee
        if they (one or more of the following):</p>
        <ul class="govuk-list govuk-list--bullet">
            <li class="govuk-!-font-size-19">are on certain benefits</li>
            <li class="govuk-!-font-size-19">have little or no savings</li>
            <li class="govuk-!-font-size-19">have low income</li>
        </ul>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("helpWithFeesNeeded")
            .pageLabel("Confirm if the defendant needs help paying fees")
            .showCondition(fieldEquals("xui_genapp_ShowHwfScreens", VerticalYesNo.YES))
            .label("helpWithFeesNeeded-lineSeparator", "---")
            .label("helpWithFeesNeeded-info", INFO_MARKDOWN)
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getNeedHwf)
            .done();
    }

}
