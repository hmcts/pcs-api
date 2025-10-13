package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

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
                       <li>Are violent or aggressive</li>
                       <li>Possess a firearm or other weapon</li>
                       <li>Have a history of criminal or antisocial behaviour</li>
                       <li>Have made verbal or written threats towards you</li>
                       <li>Are a member of a group that protests evictions</li>
                       <li>Have had visits from the police or social services</li>
                       <li>Own an aggressive dog or other animal</li>
                     </ul>
                    """)
            .mandatory(PCSCase::getConfirmLivingAtProperty)
            .done();
    }

}
