package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class ASBQuestionsWales implements CcdPageConfiguration {
    // Placeholder for Wales ASB questions page - full implementation will
    // be done in HDPI-2381

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("asbQuestionsWales")
                .pageLabel("ASB questions (Wales - placeholder)")
                .label("asbQuestionsWales-separator", "---") 
                .showCondition("showASBQuestionsPageWales=\"Yes\"")
                .readonly(PCSCase::getShowASBQuestionsPageWales, NEVER_SHOW);
    }
}
