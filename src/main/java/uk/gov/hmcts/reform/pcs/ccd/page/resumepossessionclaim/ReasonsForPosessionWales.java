package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class ReasonsForPosessionWales implements CcdPageConfiguration {
    // Placeholder for Wales reasons for possession page - full implementation will
    // be done in HDPI-2372

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("reasonsForPosessionWales")
                .pageLabel("Reasons for possession (Wales - placeholder)")
                .showCondition("showReasonsForGroundsPageWales=\"Yes\"")
                .readonly(PCSCase::getShowReasonsForGroundsPageWales, NEVER_SHOW);
    }
}
