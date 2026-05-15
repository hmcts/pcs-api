package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

@Slf4j
@AllArgsConstructor
public class StartSomethingElse implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">
        The defendant can ask the court to make a decision at any point during their case.
        This is called an application.
        </p>
        <p class="govuk-body">For example, they can use this to:</p>

        <ul class="govuk-list govuk-list--bullet">
        <li class="govuk-!-font-size-19">add someone as an extra party to their claim</li>
        <li class="govuk-!-font-size-19">ask the court not to punish them for
           breaching an order (relief from sanctions)</li>
        <li class="govuk-!-font-size-19">serve a claim outside England & Wales</li>
        <li class="govuk-!-font-size-19">transfer to the High Court for enforcement by a writ of possession</li>
        </ul>

        <div class="govuk-warning-text govuk-!-margin-bottom-1">
          <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
          <strong class="govuk-warning-text__text">
            <span class="govuk-visually-hidden">Warning</span>
            You cannot apply to suspend (stop or delay) the eviction online.
          </strong>
        </div>

        <p class="govuk-body govuk-!-margin-bottom-1">To apply by post:</p>
        <ul class="govuk-list govuk-list--bullet">
            <li class="govuk-!-font-size-19">
                <a href="https://www.gov.uk/repossession/delay-eviction" target="_blank" rel="noopener noreferrer"
                class="govuk-link">read the guidance explaining how to suspend the eviction
                 (GOV.UK, opens in a new tab)</a>
            </li>
            <li class="govuk-!-font-size-19">
                <a href="https://www.gov.uk/government/publications/form-n244-application-notice"
                target="_blank" rel="noopener noreferrer" class="govuk-link">fill in the N244 form</a>
            </li>
        </ul>

        <h2 class="govuk-heading-m">What you’ll need to apply</h2>
        <p class="govuk-body govuk-!-margin-bottom-1">
        You’ll need to know:
        </p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">if the defendant can pay the court fee, or if they need help with
           fees (help to pay court fees)</li>
          <li class="govuk-!-font-size-19">what they want the court to do</li>
          <li class="govuk-!-font-size-19">why they are asking the court to do it, for example any evidence
          they have to support their application</li>
        </ul>

        <p class="govuk-body">You may need to provide some evidence for the court.</p>

        <h2 class="govuk-heading-m">Before you start</h2>
        <p class="govuk-body">Make sure that you have all of the evidence you need to apply.</p>
        <p class="govuk-body">Once you start the application, you cannot save your progress as a draft.</p>
        <p class="govuk-body">This means that if you leave the application before submitting, we will not save your
        answers so that you can return to them later.</p>

        <h2 class="govuk-heading-m">How much it will cost</h2>
        <p class="govuk-body">It usually costs ${xui_genapp_StandardFee} to apply.</p>
        <p class="govuk-body govuk-!-margin-bottom-1">The fee will increase to ${xui_genapp_MaxFee} if:</p>
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
        <p class="govuk-body">You cannot apply to suspend (stop or delay) the eviction online.</p>
        <p class="govuk-body govuk-!-margin-bottom-1">You must apply by post:</p>

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
            .page("startSomethingElse")
            .pageLabel("Ask the court to make an order on behalf of a defendant")
            .showCondition(ShowConditions.fieldEquals("xui_genapp_ApplicationType", GenAppType.SOMETHING_ELSE))
            .label("startSomethingElse-lineSeparator", "---")
            .label("startSomethingElse-info", INFO_MARKDOWN);
    }

}
