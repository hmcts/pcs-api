package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvictionScheduleViewTest {

    @Mock
    private EvictionScheduleService evictionScheduleService;
    @Mock
    private PcsCaseEntity pcsCaseEntity;

    @InjectMocks
    private EvictionScheduleView underTest;

    @Test
    void shouldOfferRescheduleAndNoticeServedOnAScheduledWarrant() {
        EnforcementOrderEntity enforcement = enforcement(EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND);
        enforcement.setBailiffDate(LocalDateTime.parse("2026-06-25T10:00"));
        enforcement.setDispenseWithService(false);
        when(evictionScheduleService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.of(enforcement));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setEvictionScheduleFields(pcsCaseEntity, pcsCase);

        EvictionSchedule schedule = pcsCase.getEnforcementOrder().getEvictionSchedule();
        assertThat(schedule.getCanScheduleEviction()).isEqualTo(YesOrNo.NO);
        assertThat(schedule.getCanRescheduleEviction()).isEqualTo(YesOrNo.YES);
        assertThat(schedule.getCanMarkNoticeOfEvictionServed()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getEnforcementOrder().getDefendantsCanSeeEnforcement()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldOfferOnlySchedulingBeforeTheEvictionIsScheduled() {
        when(evictionScheduleService.findCurrentEnforcement(pcsCaseEntity))
            .thenReturn(Optional.of(enforcement(EnforcementStatus.AWAITING_SCHEDULING)));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setEvictionScheduleFields(pcsCaseEntity, pcsCase);

        EvictionSchedule schedule = pcsCase.getEnforcementOrder().getEvictionSchedule();
        assertThat(schedule.getCanScheduleEviction()).isEqualTo(YesOrNo.YES);
        assertThat(schedule.getCanRescheduleEviction()).isEqualTo(YesOrNo.NO);
        assertThat(schedule.getCanMarkNoticeOfEvictionServed()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldShowThatDefendantsCanSeeTheEnforcementOnceTheNoticeIsServed() {
        EnforcementOrderEntity enforcement = enforcement(EnforcementStatus.CLAIMANT_CONFIRMED);
        enforcement.setDefendantsCanSeeEnforcement(true);
        when(evictionScheduleService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.of(enforcement));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setEvictionScheduleFields(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder().getDefendantsCanSeeEnforcement()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldLeaveTheFieldsUnsetWithoutAnEnforcement() {
        when(evictionScheduleService.findCurrentEnforcement(pcsCaseEntity)).thenReturn(Optional.empty());
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setEvictionScheduleFields(pcsCaseEntity, pcsCase);

        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    @Test
    void shouldIgnoreAMissingCase() {
        underTest.setEvictionScheduleFields(null, PCSCase.builder().build());
        underTest.setEvictionScheduleFields(pcsCaseEntity, null);

        verifyNoInteractions(evictionScheduleService);
    }

    private static EnforcementOrderEntity enforcement(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setStatus(status);
        return enforcement;
    }
}
