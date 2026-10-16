package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class HearingNoticeSchedulerTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final Integer HEARING_ID = 7;

    @Mock
    private SchedulerClient schedulerClient;

    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private HearingNoticeScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);
    }

    @Test
    void usesHearingIdAsTaskId() {
        when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

        scheduler.scheduleHearingNoticeGeneration(CASE_REFERENCE, HEARING_ID);

        ArgumentCaptor<SchedulableInstance> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo(HEARING_ID.toString());
    }

    @Test
    void rescheduleForSameHearingIsNoOp() {
        when(schedulerClient.scheduleIfNotExists(any())).thenReturn(false);

        scheduler.scheduleHearingNoticeGeneration(CASE_REFERENCE, HEARING_ID);

        verify(schedulerClient).scheduleIfNotExists(any());
    }

    @Test
    void doesNotScheduleWhenRelease1Dot4Disabled() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);

        scheduler.scheduleHearingNoticeGeneration(CASE_REFERENCE, HEARING_ID);

        verifyNoInteractions(schedulerClient);
    }
}
