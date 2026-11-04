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

class RescheduleEvictionConfigurerTest extends BasePageTest {

    @Test
    void shouldAddTheEvictionDatePageUnderTheRescheduleEvictionPageId() {
        setPageUnderTest(configuredPage());

        assertThat(placedFields()).isNotEmpty()
            .allSatisfy(field -> assertThat(field.getPage()).isEqualTo(RescheduleEvictionConfigurer.PAGE_ID));
    }

    @Test
    void shouldAskWhenTheEvictionIsToBeRescheduledFor() {
        setPageUnderTest(configuredPage());

        assertThat(placedFields())
            .filteredOn(field -> "evictionDateTime".equals(field.getId()))
            .singleElement()
            .satisfies(field -> assertThat(field.getCaseEventFieldLabel())
                .isEqualTo("When do you want to reschedule the eviction for?"));
    }

    private static CcdPageConfiguration configuredPage() {
        PageBuilder pageBuilder = mock(PageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);
        ArgumentCaptor<CcdPageConfiguration> added = ArgumentCaptor.forClass(CcdPageConfiguration.class);

        new RescheduleEvictionConfigurer(mock(EvictionScheduleValidator.class)).configurePages(pageBuilder);

        verify(pageBuilder).add(added.capture());
        assertThat(added.getValue()).isInstanceOf(EvictionDatePage.class);
        return added.getValue();
    }
}
