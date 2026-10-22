package uk.gov.hmcts.reform.pcs.enforcement.ccd.page;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@AllArgsConstructor
public class EnforcementPageConfigurer implements PageConfigurer {

    private EnforcementApplicationPage enforcementApplicationPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(enforcementApplicationPage);
    }
}
