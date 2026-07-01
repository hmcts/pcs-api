package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.ClaimPackSender;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.DefencePackSender;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkPrintGenerationComponentTest {

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;
    @Mock
    private ClaimPackSender claimPackSender;
    @Mock
    private DefencePackSender defencePackSender;

    @Test
    void buildsTaskWithNightlyDefaultSchedule() {
        assertThat(componentWithSchedule("DAILY|02:00").bulkPrintTask()).isNotNull();
    }

    @Test
    void parsesFixedDelayEnvironmentSchedules() {
        assertThatCode(() -> componentWithSchedule("FIXED_DELAY|60s").bulkPrintTask()).doesNotThrowAnyException();
        assertThatCode(() -> componentWithSchedule("FIXED_DELAY|600s").bulkPrintTask()).doesNotThrowAnyException();
    }

    @Test
    void doesNothingWhenFlagOff() {
        when(featureToggleService.isEnabled(FeatureFlag.BULK_PRINT)).thenReturn(false);

        runSweep();

        verifyNoInteractions(claimActivityLogRepository, claimPackSender, defencePackSender);
    }

    @Test
    void dispatchesBothSendersPerCaseWhenFlagOn() {
        UUID caseA = UUID.randomUUID();
        UUID caseB = UUID.randomUUID();
        when(featureToggleService.isEnabled(FeatureFlag.BULK_PRINT)).thenReturn(true);
        when(claimActivityLogRepository.findCaseIdsByActivityTypeAndStatus(
            ClaimActivityType.DOCUMENTS_CREATED, ClaimActivityStatus.SUCCESS)).thenReturn(List.of(caseA, caseB));

        runSweep();

        verify(claimPackSender).sendClaimPacks(caseA);
        verify(defencePackSender).sendDefencePacks(caseA);
        verify(claimPackSender).sendClaimPacks(caseB);
        verify(defencePackSender).sendDefencePacks(caseB);
    }

    private void runSweep() {
        RecurringTask<Void> task = componentWithSchedule("DAILY|02:00").bulkPrintTask();
        task.execute(null, null);
    }

    private BulkPrintGenerationComponent componentWithSchedule(String schedule) {
        return new BulkPrintGenerationComponent(
            featureToggleService, claimActivityLogRepository, claimPackSender, defencePackSender, schedule);
    }
}
