package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class GenApplicationPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("genApplicationPlaceholder")
            .pageLabel("General Application (place holder)")
            .label("genApplicationPlaceholder-content", "---")
            .label("genApplicationPlaceholder-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
