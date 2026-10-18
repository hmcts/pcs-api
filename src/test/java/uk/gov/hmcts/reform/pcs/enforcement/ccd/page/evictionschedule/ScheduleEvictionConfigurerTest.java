package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleEvictionConfigurerTest extends BasePageTest {

    @Test
    void shouldAddTheEvictionDatePageUnderTheScheduleEvictionPageId() {
        PageBuilder pageBuilder = mock(PageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);
        ArgumentCaptor<CcdPageConfiguration> added = ArgumentCaptor.forClass(CcdPageConfiguration.class);

        new ScheduleEvictionConfigurer(mock(EvictionScheduleValidator.class)).configurePages(pageBuilder);

        verify(pageBuilder).add(added.capture());
        assertThat(added.getValue()).isInstanceOf(EvictionDatePage.class);

        setPageUnderTest(added.getValue());
        assertThat(placedFields()).isNotEmpty()
            .allSatisfy(field -> assertThat(field.getPage()).isEqualTo(ScheduleEvictionConfigurer.PAGE_ID));
    }
}
