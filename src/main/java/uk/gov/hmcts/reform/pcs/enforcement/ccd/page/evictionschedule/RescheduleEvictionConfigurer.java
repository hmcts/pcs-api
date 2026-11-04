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
    public static final String DATE_QUESTION = "When do you want to reschedule the eviction for?";

    private final EvictionScheduleValidator validator;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        // TODO HDPI-7549: list the dates the claimant said they are not available, once they are recorded
        pageBuilder.add(new EvictionDatePage(PAGE_ID, PAGE_LABEL, DATE_QUESTION, validator));
    }
}
