package uk.gov.hmcts.reform.pcs.ccd.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterStatusSyncService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LetterStatusSyncScheduledTaskTest {

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private LetterStatusSyncService letterStatusSyncService;

    @Test
    void parsesTheDefaultAndTestEnvironmentSchedules() {
        assertThatCode(() -> task("DAILY|07:00,19:00").letterStatusSyncTask()).doesNotThrowAnyException();
        assertThatCode(() -> task("FIXED_DELAY|900s").letterStatusSyncTask()).doesNotThrowAnyException();
    }

    @Test
    void syncsWhenBulkPrintIsOn() {
        when(featureToggleService.isEnabled(FeatureFlag.BULK_PRINT)).thenReturn(true);

        task("FIXED_DELAY|900s").runSync();

        verify(letterStatusSyncService).syncPendingLetters();
    }

    @Test
    void doesNothingWhenBulkPrintIsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.BULK_PRINT)).thenReturn(false);

        task("FIXED_DELAY|900s").runSync();

        verifyNoInteractions(letterStatusSyncService);
    }

    private LetterStatusSyncScheduledTask task(String schedule) {
        return new LetterStatusSyncScheduledTask(featureToggleService, letterStatusSyncService, schedule);
    }
}
