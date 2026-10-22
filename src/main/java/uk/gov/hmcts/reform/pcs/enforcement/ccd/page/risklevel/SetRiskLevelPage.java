package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.risklevel;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevelDetails;

public class SetRiskLevelPage implements CcdPageConfiguration, CcdPage {

    public static final String PAGE_LABEL = "Set risk level";
    static final String HIGH_RISK = "riskLevel=\"HIGH\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel(PAGE_LABEL)
            .label(pageKey + "-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getRiskLevelDetails)
            .mandatory(RiskLevelDetails::getRiskLevel)
            .mandatory(RiskLevelDetails::getRiskLevelReason, HIGH_RISK)
            .done()
            .done();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
