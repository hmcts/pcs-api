package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.breathingspace.BreathingSpaceApplicationPage;

@Component
@AllArgsConstructor
public class BreathingSpacePageConfigurer implements PageConfigurer {

    private BreathingSpaceApplicationPage breathingSpaceApplicationPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(breathingSpaceApplicationPage);
    }
}
