package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@AllArgsConstructor
public class ManageHearingConfigurer implements PageConfigurer {

    private final ManageHearingPage manageHearingPage;
    private final HearingDetailsPage hearingDetailsPage;
    private final CancelHearingPage cancelHearingPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(manageHearingPage)
            .add(hearingDetailsPage)
            .add(cancelHearingPage);
    }
}
