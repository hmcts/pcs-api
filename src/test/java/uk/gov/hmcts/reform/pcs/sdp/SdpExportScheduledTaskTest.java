package uk.gov.hmcts.reform.pcs.sdp;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SdpExportScheduledTaskTest {

    @Mock
    private ExportService exportService;

    @Mock
    private FeatureToggleService featureToggleService;

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
    void shouldRunExportWhenEnabledAndFeatureFlagOn() {
        when(featureToggleService.isEnabled(FeatureFlag.SDP_EXPORT)).thenReturn(true);
        RecurringTask<Void> task = component("DAILY|02:00", true).sdpExportTask();
        task.execute(null, null);

        verify(exportService).exportClaims();
    }

    @Test
    void shouldNotRunExportWhenDisabled() {
        RecurringTask<Void> task = component("DAILY|02:00", false).sdpExportTask();
        task.execute(null, null);

        verifyNoInteractions(exportService);
    }

    @Test
    void shouldNotRunExportWhenFeatureFlagOff() {
        when(featureToggleService.isEnabled(FeatureFlag.SDP_EXPORT)).thenReturn(false);
        RecurringTask<Void> task = component("DAILY|02:00", true).sdpExportTask();
        task.execute(null, null);

        verifyNoInteractions(exportService);
    }

    private SdpExportScheduledTask component(String schedule, boolean enabled) {
        return new SdpExportScheduledTask(exportService, featureToggleService, schedule, enabled);
    }
}
