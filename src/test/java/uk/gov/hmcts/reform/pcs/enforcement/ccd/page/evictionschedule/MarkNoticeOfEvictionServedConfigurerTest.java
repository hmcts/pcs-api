package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarkNoticeOfEvictionServedConfigurerTest {

    @Test
    void shouldAddTheNoticeOfEvictionServedPage() {
        PageBuilder pageBuilder = mock(PageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        new MarkNoticeOfEvictionServedConfigurer(mock(EvictionScheduleValidator.class))
            .configurePages(pageBuilder);

        verify(pageBuilder).add(isA(NoticeOfEvictionServedPage.class));
    }
}
