package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

@Slf4j
@AllArgsConstructor
public class StartSetAside implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">
        You can ask the court to set aside (cancel) an order if the defendant has a good reason. For example, if they
        were unable to attend the original court hearing, because they were ill.
        </p>

        <p class="govuk-body">
        You will need to show that the defendant has a good reason to ask the court to set aside the order, and
        they may not be successful.
        </p>

        <h2 class="govuk-heading-m">What you’ll need to apply</h2>
        <p class="govuk-body govuk-!-margin-bottom-1">
        You’ll need to know:
        </p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">if the defendant can pay the court fee, or if they need Help with
           Fees (help to pay court fees)</li>
          <li class="govuk-!-font-size-19">why the defendant is asking the court to set aside the original decision</li>
        </ul>
        <p class="govuk-body">They may need to provide some evidence for the court.</p>

        <h2 class="govuk-heading-m">Before you start</h2>
        <p class="govuk-body">Make sure that you have all of the evidence you need to apply.</p>
        <p class="govuk-body">Once you start the application, you cannot save your progress as a draft.</p>
        <p class="govuk-body">This means that if you leave the application before submitting, we will not save your
        answers so that you can return to them later.</p>

        <h2 class="govuk-heading-m">How much it will cost</h2>
        <p class="govuk-body govuk-!-margin-bottom-1">It usually costs ${xui_genapp_StandardFee} to apply. The fee will
        increase to ${xui_genapp_MaxFee} if:</p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">the defendant has already told the other party that they are making
          this application, and</li>
          <li class="govuk-!-font-size-19">the other party did not agree to it
          (this means that they objected to it)</li>
        </ul>
        <p class="govuk-body">You’ll see the final application fee before you pay.</p>

        <h2 class="govuk-heading-m">If you are worried about the defendant paying fees</h2>
        <p class="govuk-body">They may be eligible to apply for
        <a href="https://www.gov.uk/get-help-with-court-fees" target="_blank" rel="noopener noreferrer"
        class="govuk-link">help with fees (GOV.UK, opens in a new tab)</a>.
        We will ask you if they want to do this when you apply.

        <h2 class="govuk-heading-m">Apply by post</h2>
        <p class="govuk-body govuk-!-margin-bottom-1">If you’d prefer to respond by post:</p>

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
            .page("startSetAside")
            .pageLabel("Ask the court to set aside (cancel) an order on behalf of a defendant")
            .showCondition(ShowConditions.fieldEquals("xui_genapp_ApplicationType", GenAppType.SET_ASIDE))
            .label("startSetAside-lineSeparator", "---")
            .label("startSetAside-info", INFO_MARKDOWN);
    }

}
