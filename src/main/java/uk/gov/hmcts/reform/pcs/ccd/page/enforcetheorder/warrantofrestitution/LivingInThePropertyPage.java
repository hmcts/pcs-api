package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

public class LivingInThePropertyPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("livingInThePropertyRestitution")
            .pageLabel("Everyone living at the property")
            .showCondition(WARRANT_OF_RESTITUTION_FLOW)
            .label("livingInThePropertyRestitution-content", "---")
            .label(
                "livingInThePropertyRestitution-information-text", """
                    <p class="govuk-body govuk-!-font-weight-bold"> The bailiff needs to know if anyone at the property
                    poses a risk.</p>
                    <p class="govuk-body govuk-!-margin-bottom-1"> For example if they:</p>
                      <ul class="govuk-list govuk-list--bullet">
                       <li class="govuk-!-font-size-19"> are violent or aggressive</li>
                       <li class="govuk-!-font-size-19"> possess a firearm or other weapon</li>
                       <li class="govuk-!-font-size-19"> have a history of criminal or antisocial behaviour</li>
                       <li class="govuk-!-font-size-19"> have made verbal or written threats towards you</li>
                       <li class="govuk-!-font-size-19"> are a member of a group that protests evictions</li>
                       <li class="govuk-!-font-size-19"> have had visits from the police or social services</li>
                       <li class="govuk-!-font-size-19"> own an aggressive dog or other animal</li>
                     </ul>
                    """
            )
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantOfRestitutionDetails)
            .mandatory(WarrantOfRestitutionDetails::getAnyRiskToBailiff)
            .done()
            .done()
            .label("livingInThePropertyRestitution-save-and-return", SAVE_AND_RETURN);
    }
}

