package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@RequiredArgsConstructor
public class RescheduleEvictionConfigurer implements PageConfigurer {

    public static final String PAGE_ID = "rescheduleEviction";
    public static final String PAGE_LABEL = "Reschedule eviction";

    private final EvictionScheduleValidator validator;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        // TODO HDPI-7549: list the dates the claimant said they are not available, read only. The claimant's
        //  "Confirm the eviction date" journey does not record them yet, so there is nothing to show.
        pageBuilder.add(new EvictionDatePage(PAGE_ID, PAGE_LABEL, validator));
    }
}
