package uk.gov.hmcts.reform.pcs.ccd.page.breathingspace;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@AllArgsConstructor
public class LiftBreathingSpacePageConfigurer implements PageConfigurer {

    private LiftBreathingSpaceApplicationPage liftBreathingSpaceApplicationPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(liftBreathingSpaceApplicationPage);
    }
}
