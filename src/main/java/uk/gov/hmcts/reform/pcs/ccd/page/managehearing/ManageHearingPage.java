package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
public class ManageHearingPage implements CcdPageConfiguration, CcdPage {
    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("Manage hearing")
            .readonly(PCSCase::getShowManageHearingPage, NEVER_SHOW)
            .readonly(PCSCase::getSelectedHearingId, NEVER_SHOW, true)
            .showCondition("showManageHearingPage=\"YES\"")
            .label("manageHearingSeparator", "---")
            .mandatory(PCSCase::getManageHearingOption);
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
