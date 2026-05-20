package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

@Slf4j
@AllArgsConstructor
public class StartAdjourn implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">
        You can ask the court to adjourn (delay) the defendant’s court hearing (move it to a later date or time).
        This means that you can ask to change the date and time when the hearing is due to take place.
        </p>

        <p class="govuk-body">
        You will need to show that the defendant has a good reason to delay the hearing, and they may not be successful.
        </p>

        <h2 class="govuk-heading-m">What you’ll need to apply</h2>
        <p class="govuk-body govuk-!-margin-bottom-1">
        You’ll need to know:
        </p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">if the defendant can pay the court fee, or if they need Help with
           Fees (help to pay court fees)</li>
          <li class="govuk-!-font-size-19">why the defendant is asking the court to delay the hearing</li>
          <li class="govuk-!-font-size-19">when they are proposing the hearing be moved to (if applicable)</li>
        </ul>
        <p class="govuk-body">They may need to provide some evidence for the court.</p>

        <h2 class="govuk-heading-m">Before you start</h2>
        <p class="govuk-body">Make sure that you have all of the evidence you need to apply.</p>
        <p class="govuk-body">Once you start the application, you cannot save your progress as a draft.</p>
        <p class="govuk-body">This means that if you leave the application before submitting, we will not save your
        answers so that you can return to them later.</p>

        <h2 class="govuk-heading-m">How long it takes</h2>
        <p class="govuk-body">Your application will be assessed on an urgency basis and will be considered
        by a judge before the hearing date.</p>

        <h2 class="govuk-heading-m">How much it will cost</h2>
        <p class="govuk-body">It’s free to apply if the defendant’s court hearing is at least 14 days away.</p>
        <p class="govuk-body">If your court hearing is sooner, they will need to
        pay ${xui_genapp_StandardFee}.</p>
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
            .page("startAdjourn")
            .pageLabel("Ask to adjourn (delay) the court hearing on behalf of a defendant")
            .showCondition(ShowConditions.fieldEquals("xui_genapp_ApplicationType", GenAppType.ADJOURN))
            .label("startAdjourn-lineSeparator", "---")
            .label("startAdjourn-info", INFO_MARKDOWN);
    }


}
