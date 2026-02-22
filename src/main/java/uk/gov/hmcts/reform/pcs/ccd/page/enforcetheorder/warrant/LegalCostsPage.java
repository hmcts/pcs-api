package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.LEGAL_COSTS_HELP;

public class LegalCostsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("legalCosts")
                .pageLabel("Legal costs")
                .showCondition(ShowConditionsEnforcementType.WARRANT_FLOW)
                .label("legalCosts-line-separator", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWarrantDetails)
                .complex(WarrantDetails::getLegalCosts)
                .mandatory(LegalCosts::getAreLegalCostsToBeClaimed)
                .mandatory(LegalCosts::getAmountOfLegalCosts,
                        "warrantAreLegalCostsToBeClaimed=\"YES\"")
                .done()
                .label("legalCosts-help", LEGAL_COSTS_HELP)
                .label("legalCosts-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
