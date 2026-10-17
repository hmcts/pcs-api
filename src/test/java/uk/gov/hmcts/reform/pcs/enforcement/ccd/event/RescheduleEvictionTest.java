package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.RescheduleEvictionConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionDetailsService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionDetailsService.EvictionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class RescheduleEvictionTest extends BaseEventTest {

    private static final Instant NOW = Instant.parse("2026-06-27T09:00:00Z");

    @Mock
    private RescheduleEvictionConfigurer rescheduleEvictionConfigurer;
    @Mock
    private EvictionDetailsService evictionDetailsService;
    @Mock
    private EvictionScheduleService evictionScheduleService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new RescheduleEviction(
            rescheduleEvictionConfigurer, evictionDetailsService,
            new EvictionScheduleValidator(Clock.fixed(NOW, UK_ZONE_ID)), new CaseReferenceFormatter(),
            evictionScheduleService));
    }

    @Test
    void shouldBeGatedOnTheRescheduleFlagAndTheEnforcementFeatureFlag() {
        assertConfiguredShowConditions("featureFlags.enforcementEnabled=\"YES\" AND canRescheduleEviction=\"Yes\"");
        assertThat(configuredEvent.getName()).isEqualTo("Reschedule eviction");
        assertThat(configuredEvent.isShowSummary()).isTrue();
        verify(rescheduleEvictionConfigurer).configurePages(any());
    }

    @Test
    void shouldGrantCrudToBailiffs() {
        assertGrants(UserRole.BAILIFF, Permission.CRUD);
    }

    @Test
    void shouldRefuseToStartBeforeTheClaimantHasAnswered() {
        doThrow(new EnforcementEventNotAvailableException(EvictionScheduleService.CANNOT_RESCHEDULE_ERROR))
            .when(evictionScheduleService).checkCanReschedule(TEST_CASE_REFERENCE);

        assertThatThrownBy(() -> callStartHandler(PCSCase.builder().build()))
            .isInstanceOf(EnforcementEventNotAvailableException.class);
    }

    @Test
    void shouldAskTheQuestionsAgainWithDispenseWithServiceDefaultingToNo() {
        when(evictionDetailsService.getEvictionDetails(TEST_CASE_REFERENCE))
            .thenReturn(new EvictionDetails("Private landlord", List.of("Acme"), List.of("Jo Bloggs")));

        PCSCase result = callStartHandler(PCSCase.builder().build());

        EvictionSchedule schedule = result.getEnforcementOrder().getEvictionSchedule();
        assertThat(schedule.getEvictionDispenseWithService()).isEqualTo(YesOrNo.NO);
        assertThat(schedule.getEvictionDetailsMarkup()).contains("Jo Bloggs");
        verify(evictionScheduleService).checkCanReschedule(TEST_CASE_REFERENCE);
    }

    @Test
    void shouldRescheduleAndConfirm() {
        when(evictionScheduleService.reschedule(anyLong(), any(), anyBoolean()))
            .thenReturn(EnforcementChange.made("WP-00000042"));

        SubmitResponse<State> response = callSubmitHandler(caseWith(LocalDateTime.parse("2026-07-01T09:30")));

        verify(evictionScheduleService).reschedule(TEST_CASE_REFERENCE, LocalDateTime.parse("2026-07-01T09:30"),
                                                   false);
        assertThat(response.getConfirmationHeader())
            .isEqualTo("# Eviction date rescheduled for Wednesday 1 July 2026 at 09:30am");
        assertThat(response.getConfirmationBody()).contains("Warrant number: WP-00000042");
    }

    @Test
    void shouldReturnAnErrorWhenTheWarrantHasMovedOn() {
        when(evictionScheduleService.reschedule(anyLong(), any(), anyBoolean()))
            .thenReturn(EnforcementChange.refused(EvictionScheduleService.CANNOT_RESCHEDULE_ERROR));

        SubmitResponse<State> response = callSubmitHandler(caseWith(LocalDateTime.parse("2026-07-01T09:30")));

        assertThat(response.getErrors()).containsExactly(EvictionScheduleService.CANNOT_RESCHEDULE_ERROR);
        assertThat(response.getConfirmationHeader()).isNull();
    }

    @Test
    void shouldRejectADateInThePast() {
        SubmitResponse<State> response = callSubmitHandler(caseWith(LocalDateTime.parse("2026-06-26T09:30")));

        assertThat(response.getErrors()).containsExactly("The eviction date must be today or in the future");
        verify(evictionScheduleService, never()).reschedule(anyLong(), any(), anyBoolean());
    }

    private static PCSCase caseWith(LocalDateTime evictionDateTime) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .evictionSchedule(EvictionSchedule.builder()
                                                        .evictionDateTime(evictionDateTime)
                                                        .evictionDispenseWithService(YesOrNo.NO)
                                                        .build())
                                  .build())
            .build();
    }
}
