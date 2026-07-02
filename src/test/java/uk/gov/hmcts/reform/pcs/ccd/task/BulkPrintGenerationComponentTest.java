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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
        assertThat(component("DAILY|02:00", null).bulkPrintTask()).isNotNull();
    }

    @Test
    void parsesFixedDelayEnvironmentSchedules() {
        assertThatCode(() -> component("FIXED_DELAY|60s", null).bulkPrintTask()).doesNotThrowAnyException();
        assertThatCode(() -> component("FIXED_DELAY|600s", null).bulkPrintTask()).doesNotThrowAnyException();
    }

    @Test
    void doesNothingWhenFlagOff() {
        when(featureToggleService.isEnabled(FeatureFlag.BULK_PRINT)).thenReturn(false);

        runSweep(null);

        verifyNoInteractions(claimActivityLogRepository, claimPackSender, defencePackSender);
    }

    @Test
    void dispatchesBothSendersPerCaseFromFullBacklogWhenNoLookback() {
        UUID caseA = UUID.randomUUID();
        UUID caseB = UUID.randomUUID();
        when(featureToggleService.isEnabled(FeatureFlag.BULK_PRINT)).thenReturn(true);
        when(claimActivityLogRepository.findCaseIdsByActivityTypeAndStatus(
            ClaimActivityType.DOCUMENTS_CREATED, ClaimActivityStatus.SUCCESS)).thenReturn(List.of(caseA, caseB));

        runSweep(null);

        verify(claimActivityLogRepository, never())
            .findCaseIdsByActivityTypeAndStatusCreatedAfter(any(), any(), any());
        verify(claimPackSender).sendClaimPacks(caseA);
        verify(defencePackSender).sendDefencePacks(caseA);
        verify(claimPackSender).sendClaimPacks(caseB);
        verify(defencePackSender).sendDefencePacks(caseB);
    }

    @Test
    void appliesLookbackCutoffWhenConfigured() {
        UUID caseId = UUID.randomUUID();
        when(featureToggleService.isEnabled(FeatureFlag.BULK_PRINT)).thenReturn(true);
        when(claimActivityLogRepository.findCaseIdsByActivityTypeAndStatusCreatedAfter(
            eq(ClaimActivityType.DOCUMENTS_CREATED), eq(ClaimActivityStatus.SUCCESS), any(LocalDateTime.class)))
            .thenReturn(List.of(caseId));

        runSweep(24);

        verify(claimActivityLogRepository).findCaseIdsByActivityTypeAndStatusCreatedAfter(
            eq(ClaimActivityType.DOCUMENTS_CREATED), eq(ClaimActivityStatus.SUCCESS), any(LocalDateTime.class));
        verify(claimActivityLogRepository, never()).findCaseIdsByActivityTypeAndStatus(any(), any());
        verify(claimPackSender).sendClaimPacks(caseId);
        verify(defencePackSender).sendDefencePacks(caseId);
    }

    private void runSweep(Integer lookbackHours) {
        RecurringTask<Void> task = component("DAILY|02:00", lookbackHours).bulkPrintTask();
        task.execute(null, null);
    }

    private BulkPrintGenerationComponent component(String schedule, Integer lookbackHours) {
        return new BulkPrintGenerationComponent(
            featureToggleService, claimActivityLogRepository, claimPackSender, defencePackSender,
            schedule, lookbackHours);
    }
}
