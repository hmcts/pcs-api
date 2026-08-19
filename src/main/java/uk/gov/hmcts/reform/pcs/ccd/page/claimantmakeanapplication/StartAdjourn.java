package uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication;

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
        You can ask the court to adjourn (delay) the court hearing (move it to a later date or time).
        This means that you can ask to change the date and time when the hearing is due to take place.
        </p>

        <p class="govuk-body">
        You will need to provide a good reason to delay the hearing, and you may not be successful.
        </p>

        <h2 class="govuk-heading-m">What you’ll need to apply</h2>
        <p class="govuk-body govuk-!-margin-bottom-1">
        You’ll need to know:
        </p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">why you are asking the court to delay the hearing</li>
          <li class="govuk-!-font-size-19">when you are proposing the hearing be moved to (if applicable)</li>
        </ul>
        <p class="govuk-body">You may need to provide some evidence for the court.</p>

        <h2 class="govuk-heading-m">Before you start</h2>
        <p class="govuk-body">Make sure that you have all of the evidence you need to apply.</p>
        <p class="govuk-body">Once you start the application, you cannot save your progress as a draft.</p>
        <p class="govuk-body">This means that if you leave the application before submitting, we will not save your
        answers so that you can return to them later.</p>

        <h2 class="govuk-heading-m">How long it takes</h2>
        <p class="govuk-body">Your application will be assessed on an urgency basis and will be considered
        by a judge before the hearing date.</p>

        <h2 class="govuk-heading-m">How much it will cost</h2>
        <p class="govuk-body">It’s free to apply if the court hearing is at least 14 days away.</p>
        <p class="govuk-body">If your court hearing is sooner, you will need to
        pay ${xui_genapp_StandardFee}.</p>
        <p class="govuk-body govuk-!-margin-bottom-1">The fee will increase to ${xui_genapp_MaxFee} if:</p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">you have already told the other party that you are making
          this application, and</li>
          <li class="govuk-!-font-size-19">the other party did not agree to it
          (this means that they objected to it)</li>
        </ul>
        <p class="govuk-body">You’ll see the final application fee before you pay.</p>
        <h2 class="govuk-heading-m">Apply by post</h2>
        <p class="govuk-body">You cannot apply to suspend (stop or delay) the eviction online.</p>
        <p class="govuk-body govuk-!-margin-bottom-1">You must apply by post:</p>

        <ul class="govuk-list govuk-list--bullet">
            <li class="govuk-!-font-size-19">
                <a href="https://www.gov.uk/government/publications/form-n244-application-notice"
                target="_blank" rel="noopener noreferrer" class="govuk-link">fill in the N244 form</a>
            </li>
            <li class="govuk-!-font-size-19"><a href="https://www.gov.uk/find-court-tribunal" target="_blank"
                rel="noopener noreferrer" class="govuk-link">find your local court</a>
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
            .pageLabel("Ask to adjourn (delay) the court hearing")
            .showCondition(ShowConditions.fieldEquals("xui_genapp_ApplicationType", GenAppType.ADJOURN))
            .label("startAdjourn-lineSeparator", "---")
            .label("startAdjourn-info", INFO_MARKDOWN);
    }


}
