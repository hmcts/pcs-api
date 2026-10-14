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
    void shouldRunFullExportWhenEnabledAndFullLoad() {
        runTask(true, true);

        verify(exportService).exportAllClaims();
    }

    @Test
    void shouldNotRunExportWhenDisabled() {
        runTask(false, true);

        verifyNoInteractions(exportService);
    }

    @Test
    void shouldRunDeltaExportWhenEnabledWithoutFullLoad() {
        runTask(true, false);

        verify(exportService).exportDeltaClaims();
    }

    private void runTask(boolean enabled, boolean fullLoad) {
        RecurringTask<Void> task = component("DAILY|02:00", enabled, fullLoad).sdpExportTask();
        task.execute(null, null);
    }

    private SdpExportScheduledTask component(String schedule, boolean enabled) {
        return component(schedule, enabled, false);
    }

    private SdpExportScheduledTask component(String schedule, boolean enabled, boolean fullLoad) {
        return new SdpExportScheduledTask(exportService, schedule, enabled, fullLoad);
    }
}
