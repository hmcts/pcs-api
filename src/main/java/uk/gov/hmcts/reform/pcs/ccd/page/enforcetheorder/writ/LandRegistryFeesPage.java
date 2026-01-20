package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class LandRegistryFeesPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("landRegistryFeesWrit")
                .pageLabel("Land Registry fees")
                .showCondition(ShowConditionsWarrantOrWrit.WRIT_FLOW)
                .label("landRegistryFeesWrit-content", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWritDetails)
                .complex(WritDetails::getLandRegistryFees)
                .mandatory(LandRegistryFees::getHaveLandRegistryFeesBeenPaid)
                .mandatory(LandRegistryFees::getAmountOfLandRegistryFees, "writHaveLandRegistryFeesBeenPaid=\"YES\"")
                    .done()
                .done()
                .label("landRegistryFeesWrit-save-and-return", SAVE_AND_RETURN);
    }
}
