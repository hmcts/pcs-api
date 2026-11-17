package uk.gov.hmcts.reform.pcs.ccd.page.breathingspace;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

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
