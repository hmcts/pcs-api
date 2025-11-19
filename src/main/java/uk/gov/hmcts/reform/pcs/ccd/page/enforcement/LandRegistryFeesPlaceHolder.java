package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class LandRegistryFeesPlaceHolder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("landRegistryFeesPlaceHolder")
                .pageLabel("Land Registry Fees (place holder)")
                .label("landRegistryFeesPlaceHolder-content", "---")
                .label("legalCosts-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
