package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class LivingInThePropertyPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("livingInTheProperty")
            .pageLabel("Everyone living at the property")
            .showCondition("selectEnforcementType=\"WARRANT\"")
            .label("livingInTheProperty-content", "---")
            .label(
                "livingInTheProperty-information-text", """
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
            .complex(EnforcementOrder::getWarrantDetails)
            .mandatory(WarrantDetails::getAnyRiskToBailiff)
            .done()
            .label("livingInTheProperty-save-and-return", SAVE_AND_RETURN);
    }
}
