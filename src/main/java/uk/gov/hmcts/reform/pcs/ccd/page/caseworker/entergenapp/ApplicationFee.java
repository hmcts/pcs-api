package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class ApplicationFee implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("applicationFee")
            .pageLabel("Application fee")
            .label("applicationFee-lineSeparator", "---")
            .label("applicationFee-placeholder", "Application fee details(placeholder)");
    }

}
