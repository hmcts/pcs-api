package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class SuspensionOfRightToBuyOrderReason implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("suspensionOrderReason")
            .pageLabel("Reasons for requesting a suspension order")
            .showCondition("alternativesToPossessionCONTAINS\"SUSPENSION_OF_RIGHT_TO_BUY\"")
            .label("suspensionOrderReason-info", "---")
            .mandatory(PCSCase::getSuspensionOfRightToBuyReason);
    }
}
