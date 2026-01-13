package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class EnforcementOfficerPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("enforcementOfficerPlaceHolder")
            .pageLabel("Your High Court enforcement officer (place holder)")
            .showCondition("selectEnforcementType=\"WRIT\"")
            .label("enforcementOfficerPlaceHolder-content", "---")
            .label("enforcementOfficerPlaceHolder-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
