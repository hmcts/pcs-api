package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class UploadGeneralApplication implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadGeneralApplication")
            .pageLabel("Upload general application")
            .label("uploadGeneralApplication-lineSeparator", "---")
            .mandatory(PCSCase::getUploadSingleDocument);
    }

}
