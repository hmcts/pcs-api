package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

/**
 * CCD page configuration for making a housing possession claim online.
 */
public class WelshTranslationPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("singleStringTest")
            .label("singleStringContent", "TEST-S002.1-1-tNqmWGciWm");
    }
}
