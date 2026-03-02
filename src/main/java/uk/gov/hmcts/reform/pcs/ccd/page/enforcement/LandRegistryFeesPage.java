package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.LandRegistryFees;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class LandRegistryFeesPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("landRegistryFees")
                .pageLabel("Land Registry fees")
                .label("landRegistryFees-content", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getLandRegistryFees)
                    .mandatory(LandRegistryFees::getHaveLandRegistryFeesBeenPaid)
                    .mandatory(
                        LandRegistryFees::getAmountOfLandRegistryFees,
                        "haveLandRegistryFeesBeenPaid=\"YES\"")
                    .done()
                .done()
                .label("landRegistryFees-save-and-return", SAVE_AND_RETURN);
    }

}
