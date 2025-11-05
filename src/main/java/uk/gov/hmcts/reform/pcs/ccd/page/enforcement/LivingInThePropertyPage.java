package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class LivingInThePropertyPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("livingInTheProperty")
            .pageLabel("Everyone living at the property")
            .label("livingInTheProperty-content", "---")
            .label(
                "livingInTheProperty-information-text", """
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
            .complex(PCSCase::getEnforcementOrder)
            .mandatory(EnforcementOrder::getAnyRiskToBailiff)
            .done()
            .label("livingInTheProperty-save-and-return", SAVE_AND_RETURN);
    }
}
