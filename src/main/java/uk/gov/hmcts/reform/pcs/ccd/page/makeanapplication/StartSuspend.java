package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

@Slf4j
@AllArgsConstructor
public class StartSuspend implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">
        You can ask a judge to suspend a warrant or a writ of possession. This means delaying the eviction or allowing
        the defendant to stay in their home if they're able to make payments again.
        </p>

        <p class="govuk-body">
        A new hearing will be held but the judge will not automatically agree to suspend the possession warrant or
        writ - it depends what happens in court.
        </p>

        <p class="govuk-body govuk-!-font-weight-bold">If the court approves the request to suspend the eviction</p>

        <p class="govuk-body">
        If the court approves the request the defendant may be able to stay in their home, either temporarily or
        indefinitely, if they meet certain conditions. For example the judge could ask them to set up a payment plan so
        that they can repay the rent or mortgage payments they owe. If they do not meet the conditions,they can be
        evicted.
        </p>

        <p class="govuk-body govuk-!-font-weight-bold">If the court refuses the request to suspend the eviction</p>

        <p class="govuk-body">
        If the court refuses the request, the defendant will need to leave the property.
        </p>

        <h2 class="govuk-heading-m">What you’ll need to apply</h2>
        <p class="govuk-body govuk-!-margin-bottom-1">
        You’ll need to know:
        </p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">if the defendant can pay the court fee, or if they need Help with
           Fees (help to pay court fees)</li>
          <li class="govuk-!-font-size-19">why the defendant is asking the court to suspend the warrant</li>
        </ul>
        <p class="govuk-body">They may need to provide some evidence for the court.</p>

        <h2 class="govuk-heading-m">Before you start</h2>
        <p class="govuk-body">Make sure that you have all of the evidence you need to apply.</p>
        <p class="govuk-body">Once you start the application, you cannot save your progress as a draft.</p>
        <p class="govuk-body">This means that if you leave the application before submitting, we will not save your
        answers so that you can return to them later.</p>

        <h2 class="govuk-heading-m">How much it will cost</h2>
        <p class="govuk-body">It costs £15 / ${xui_genapp_StandardFee} to apply to suspend an eviction.</p>

        <h2 class="govuk-heading-m">If you are worried about the defendant paying fees</h2>
        <p class="govuk-body">They may be eligible to apply for
        <a href="https://www.gov.uk/get-help-with-court-fees" target="_blank" rel="noopener noreferrer"
        class="govuk-link">help with fees (GOV.UK, opens in a new tab)</a>.
        We will ask you if they want to do this when you apply.

        <h2 class="govuk-heading-m">Apply by post</h2>
        <p class="govuk-body govuk-!-margin-bottom-1">If you'd prefer to respond by post:</p>

        <ul class="govuk-list govuk-list--bullet">
            <li class="govuk-!-font-size-19">
                <a href="https://www.gov.uk/government/publications/form-n244-application-notice"
                target="_blank" rel="noopener noreferrer" class="govuk-link">fill in the N244 form</a>
            </li>
            <li class="govuk-!-font-size-19"><a href="https://www.gov.uk/find-court-tribunal" target="_blank"
                rel="noopener noreferrer" class="govuk-link">find the defendant’s local court</a>
            </li>
            <li class="govuk-!-font-size-19">
                send the completed form to the court
            </li>
        </ul>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("startSuspend")
            .pageLabel("Ask the court to (stop or suspend) the eviction on behalf of a defendant")
            .showCondition(ShowConditions.fieldEquals("xui_genapp_ApplicationType", GenAppType.SUSPEND))
            .label("startSuspend-lineSeparator", "---")
            .label("startSuspend-info", INFO_MARKDOWN);
    }


}
