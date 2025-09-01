package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class ReasonForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("reasonForPossession")
            .pageLabel("Reasons for possession")
            .readonly(PCSCase::getSelectedSecureOrFlexibleDiscretionaryGrounds,NEVER_SHOW)
            .readonly(PCSCase::getRentAreasOrBreachOfTenancy,NEVER_SHOW)
        .showCondition("selectedSecureOrFlexibleDiscretionaryGrounds!=\"RENT_ARREARS_OR_BREACH_OF_TENANCY\"" +
                           " OR rentAreasOrBreachOfTenancy=\"BREACH_OF_TENANCY\"" +
                           " OR selectedSecureOrFlexibleDiscretionaryGrounds=\"*\"")
            .label("reasonForPossession-info2", """
                ---
               <h2>Nuisance, annoyance, illegal or immoral use of the property (ground 2)</h2
                """
                ,"selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS\"NUISANCE_OR_IMMORAL_USE\"")
            .mandatory(
                PCSCase::getNuisancePossessionReason
               ,       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS\"NUISANCE_OR_IMMORAL_USE\"")
            .mandatory(PCSCase::getBreachOfTenancyPossessionReason, "rentAreasOrBreachOfTenancy=\"BREACH_OF_TENANCY\"");

    }
}
