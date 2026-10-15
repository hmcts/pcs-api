package uk.gov.hmcts.reform.pcs.sdp;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SdpExportScheduledTaskTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-15T02:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ExportService exportService;

    @Test
    void shouldBuildTaskWithNightlyDefaultSchedule() {
        assertThat(component("DAILY|02:00", true, "").sdpExportTask()).isNotNull();
    }

    @Test
    void shouldParseFixedDelayEnvironmentSchedule() {
        assertThatCode(() -> component("FIXED_DELAY|60s", true, "").sdpExportTask())
            .doesNotThrowAnyException();
    }

    @Test
    void shouldRunDeltaExportWhenEnabled() {
        RecurringTask<Void> task = component("DAILY|02:00", true, "").sdpExportTask();
        task.execute(null, null);

        verify(exportService).exportDeltaClaims();
        verify(exportService, never()).exportAllClaims();
    }

    @Test
    void shouldRunFullExportOnFullLoadDate() {
        RecurringTask<Void> task = component("DAILY|02:00", true, "2026-10-15").sdpExportTask();
        task.execute(null, null);

        verify(exportService).exportAllClaims();
        verify(exportService, never()).exportDeltaClaims();
    }

    @Test
    void shouldRunDeltaExportWhenFullLoadDateIsNotToday() {
        RecurringTask<Void> task = component("DAILY|02:00", true, "2026-10-16").sdpExportTask();
        task.execute(null, null);

        verify(exportService).exportDeltaClaims();
        verify(exportService, never()).exportAllClaims();
    }

    @Test
    void shouldNotRunExportWhenDisabled() {
        RecurringTask<Void> task = component("DAILY|02:00", false, "").sdpExportTask();
        task.execute(null, null);

        verifyNoInteractions(exportService);
    }

    private SdpExportScheduledTask component(String schedule, boolean enabled, String fullLoadDate) {
        return new SdpExportScheduledTask(exportService, schedule, enabled, fullLoadDate, FIXED_CLOCK);
    }
}
