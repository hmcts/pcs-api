package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class EnforcementApplicationPage implements CcdPageConfiguration {
    public static final String WRIT_OR_WARRANT_INFORMATION = """
                    <details class="govuk-details">
                        <summary class="govuk-details__summary">
                            <span class="govuk-details__summary-text">
                                I do not know if I need a writ or a warrant
                            </span>
                        </summary>
                        <div class="govuk-details__text">
                            <p class="govuk-body"> You can use either a warrant or a writ to evict someone.</p>
                            <p class="govuk-body govuk-!-font-weight-bold"> If you choose a warrant of possession</p>
                            <p class="govuk-body govuk-!-margin-bottom-1"> If you apply for a warrant:</p>
                            <ul class="govuk-list govuk-list--bullet">
                              <li class="govuk-!-font-size-19"> It costs ${warrantFeeAmount} to apply </li>
                              <li class="govuk-!-font-size-19"> It's free to hire a County Court bailiff </li>
                              <li class="govuk-!-font-size-19"> you may have to wait longer for the eviction
                              (this depends on the court you apply to)
                               </li>
                            </ul>
                            <p class="govuk-body govuk-!-font-weight-bold"> If you ask the judge for a writ of
                            possession</p>
                            <p class="govuk-body govuk-!-margin-bottom-1"> It's usually faster to evict someone using a
                            writ, but:</p>
                            <ul class="govuk-list govuk-list--bullet">
                              <li class="govuk-!-font-size-19"> It's more difficult to prove that you need a writ </li>
                              <li class="govuk-!-font-size-19"> It's an extra step in the legal process </li>
                            </ul>
                            <p class="govuk-body govuk-!-margin-bottom-1"> You'll need to:</p>
                            <ul class="govuk-list govuk-list--bullet">
                              <li class="govuk-!-font-size-19"> ask the judge for a writ (you’ll need additional
                              evidence for this and the judge could still refuse your request)</li>
                              <li class="govuk-!-font-size-19"> hire a private High Court Enforcement Officer (bailiff)
                              to carry out the eviction</li>
                            </ul>
                            <p class="govuk-body govuk-!-margin-bottom-1"> If you apply for a writ:</p>
                            <ul class="govuk-list govuk-list--bullet">
                              <li class="govuk-!-font-size-19"> It costs ${writFeeAmount} to apply</li>
                              <li class="govuk-!-font-size-19"> <a href="https://www.hceoa.org.uk/choosing-a-hceo"
                              rel="noreferrer noopener"
                              target="_blank" class="govuk-link"> You can get a quote from a bailiff to find out how
                              much it will cost</a> (they usually charge by the hour)</li>
                              <li class="govuk-!-font-size-19"> It’s usually faster (6 to 12 weeks, depending on the
                              court you apply to)</li>
                            </ul>
                            <p class="govuk-body govuk-!-font-weight-bold"> The additional evidence you’ll need to
                            apply for a writ</p>
                            <p class="govuk-body govuk-!-margin-bottom-1"> You’ll need to show that either:</p>
                            <ul class="govuk-list govuk-list--bullet">
                              <li class="govuk-!-font-size-19"> there is a backlog in the County Court bailiff service
                              </li>
                              <li class="govuk-!-font-size-19"> the defendant (the person or people living at the
                              property) has damaged your
                              property</li>
                              <li class="govuk-!-font-size-19 govuk-!-padding-bottom-1"> you are suffering financially
                              due to lost income</li>
                            </ul>
                            <p class="govuk-body"> If you do not have this evidence the judge could deny your request.
                            If they deny it, you’ll need to start again (re-apply for the writ or the warrant with new
                            evidence).</p>
                            <p class="govuk-body"> Contact a lawyer or a High Court Enforcement Officer (bailiff)
                            before you apply for a writ. They can help you to check if you have the evidence to apply
                            successfully.</p>
                        </div>
                    </details>
                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("enforcementApplication")
                .pageLabel("Your application")
                .label("enforcementApplication-content", "---")
                .complex(PCSCase::getEnforcementOrder)
                .mandatory(EnforcementOrder::getSelectEnforcementType)
                .readonly(EnforcementOrder::getWarrantFeeAmount, NEVER_SHOW, true)
                .readonly(EnforcementOrder::getWritFeeAmount, NEVER_SHOW, true)
                .label("enforcementApplication-clarification", WRIT_OR_WARRANT_INFORMATION);
    }
}
