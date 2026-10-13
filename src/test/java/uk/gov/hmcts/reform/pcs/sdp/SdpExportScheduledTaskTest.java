package uk.gov.hmcts.reform.pcs.sdp;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SdpExportScheduledTaskTest {

    @Mock
    private ExportService exportService;

    @Test
    void shouldBuildTaskWithNightlyDefaultSchedule() {
        assertThat(component("DAILY|02:00", true).sdpExportTask()).isNotNull();
    }

    @Test
    void shouldParseFixedDelayEnvironmentSchedule() {
        assertThatCode(() -> component("FIXED_DELAY|60s", true).sdpExportTask())
            .doesNotThrowAnyException();
    }

    @Test
    void shouldRunExportWhenEnabled() {
        runTask(true);

        verify(exportService).exportAllClaims();
    }

    @Test
    void shouldNotRunExportWhenDisabled() {
        runTask(false);

        verifyNoInteractions(exportService);
    }

    private void runTask(boolean enabled) {
        RecurringTask<Void> task = component("DAILY|02:00", enabled).sdpExportTask();
        task.execute(null, null);
    }

    private SdpExportScheduledTask component(String schedule, boolean enabled) {
        return new SdpExportScheduledTask(exportService, schedule, enabled);
    }
}
