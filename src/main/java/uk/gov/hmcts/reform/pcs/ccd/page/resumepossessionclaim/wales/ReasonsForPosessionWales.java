package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class ReasonsForPosessionWales implements CcdPageConfiguration {
    // Placeholder for Wales reasons for possession page - full implementation will
    // be done in HDPI-2435

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("reasonsForPosessionWales")
                .pageLabel("Reasons for possession (Wales - placeholder)")
                .label("reasonsForPosessionWales-separator", "---")
                .showCondition("legislativeCountry=\"Wales\" AND showReasonsForGroundsPageWales=\"Yes\"")
                .readonly(PCSCase::getShowReasonsForGroundsPageWales, NEVER_SHOW)
                .label("reasonsForPosessionWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
