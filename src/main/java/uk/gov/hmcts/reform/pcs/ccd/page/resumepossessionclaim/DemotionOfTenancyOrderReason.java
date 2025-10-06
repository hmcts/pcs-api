package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;

public class DemotionOfTenancyOrderReason implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("demotionOfTenancyOrderReason")
            .pageLabel("Reasons for requesting a demotion order")
            .showCondition("showDemotionOfTenancyHousingActsPage=\"Yes\"")
            .label("demotionOfTenancyOrderReason-info", "---")
                .complex(PCSCase::getDemotionOfTenancy)
                .mandatory(DemotionOfTenancy::getDemotionOfTenancyReason)
                .done();
    }
}

