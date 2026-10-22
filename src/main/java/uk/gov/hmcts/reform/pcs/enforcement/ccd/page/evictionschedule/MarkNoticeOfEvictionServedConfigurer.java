package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

@Component
@RequiredArgsConstructor
public class MarkNoticeOfEvictionServedConfigurer implements PageConfigurer {

    private final EvictionScheduleValidator validator;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder.add(new NoticeOfEvictionServedPage(validator));
    }
}
