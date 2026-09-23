package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

@Component
public class EditHearingPage implements CcdPageConfiguration, CcdPage {
    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .showCondition("manageHearingOption=\"EDIT\"")
            .pageLabel("Edit a hearing")
            .label("placeholderEditHearing", "PLACEHOLDER PAGE");
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
