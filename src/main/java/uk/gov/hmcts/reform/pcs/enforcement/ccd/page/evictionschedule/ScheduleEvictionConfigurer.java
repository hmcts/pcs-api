package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@RequiredArgsConstructor
public class ScheduleEvictionConfigurer implements PageConfigurer {

    public static final String PAGE_ID = "scheduleEviction";
    public static final String PAGE_LABEL = "Schedule eviction";

    private final EvictionScheduleValidator validator;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder.add(new EvictionDatePage(PAGE_ID, PAGE_LABEL, validator));
    }
}
