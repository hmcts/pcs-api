package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

public interface EnforcementPageConfigurer extends PageConfigurer {

    default void configureInitialPages(PageBuilder pageBuilder) {
        pageBuilder.add(new EnforcementApplicationPage());
    }
}
