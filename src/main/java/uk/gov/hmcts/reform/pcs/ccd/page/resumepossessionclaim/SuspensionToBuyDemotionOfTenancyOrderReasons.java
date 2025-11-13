package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class SuspensionToBuyDemotionOfTenancyOrderReasons implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("suspensionToBuyDemotionOfTenancyOrderReasons")
            .pageLabel("Reasons for requesting a suspension order and a demotion order")
            .showCondition("suspensionToBuyDemotionOfTenancyPages=\"Yes\"")
            .label("suspensionToBuyDemotionOfTenancyOrderReasons-info", "---")
            .complex(PCSCase::getSuspensionOfRightToBuyDemotionOfTenancy)
                .mandatory(SuspensionOfRightToBuyDemotionOfTenancy::getSuspensionOrderReason)
                .mandatory(SuspensionOfRightToBuyDemotionOfTenancy::getDemotionOrderReason)
            .done()
            .label("suspensionToBuyDemotionOfTenancyOrderReasons-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
