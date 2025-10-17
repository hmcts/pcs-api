package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class LivingInThePropertyPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("livingInThePropertyPage")
            .pageLabel("Everyone living at the property")
            .label("livingInThePropertyPage-content", "---")
            .label(
                "livingInThePropertyPage", """
                    <p>The bailiff needs to know if anyone at the property poses a risk.</p>
                    <p>For example if they:</p>
                      <ul>
                       <li>are violent or aggressive</li>
                       <li>possess a firearm or other weapon</li>
                       <li>have a history of criminal or antisocial behaviour</li>
                       <li>have made verbal or written threats towards you</li>
                       <li>are a member of a group that protests evictions</li>
                       <li>have had visits from the police or social services</li>
                       <li>own an aggressive dog or other animal</li>
                     </ul>
                    """
            )
            .mandatory(PCSCase::getConfirmLivingAtProperty)
            .label(
                "livingInThePropertyPage-link", """
                    <details class="govuk-details">
                        <summary class="govuk-details__summary">
                            <span class="govuk-details__summary-text">
                                I want to save this application and return to it later
                            </span>
                        </summary>
                        <div class="govuk-details__text">
                            If you want to save your application and return to it later:
                            <ol class="govuk-list govuk-list--number">
                                <li>Choose 'Continue'</li>
                                <li>On the next page choose 'Cancel'</li>
                            </ol>
                            This will save your progress and take you to your case list.
                        </div>
                    </details>"""
            )
            .done();
    }
}
