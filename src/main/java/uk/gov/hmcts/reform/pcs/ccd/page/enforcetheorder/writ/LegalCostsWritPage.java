package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.LEGAL_COSTS_HELP;

public class LegalCostsWritPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("legalCostsWrit")
                .pageLabel("Legal costs")
                .showCondition("selectEnforcementType=\"WRIT\"")
                .label("legalCostsWrit-line-separator", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWritDetails)
                .complex(WritDetails::getLegalCosts)
                .mandatory(LegalCosts::getAreLegalCostsToBeClaimed)
                .mandatory(LegalCosts::getAmountOfLegalCosts,
                        "writAreLegalCostsToBeClaimed=\"YES\"")
                .done()
                .label("legalCostsWrit-help", LEGAL_COSTS_HELP)
                .label("legalCostsWrit-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
