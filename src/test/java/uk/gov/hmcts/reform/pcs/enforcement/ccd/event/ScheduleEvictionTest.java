package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.ScheduleEvictionConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
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
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class ScheduleEvictionTest extends BaseEventTest {

    private static final Instant NOW = Instant.parse("2026-06-20T09:00:00Z");

    @Mock
    private ScheduleEvictionConfigurer scheduleEvictionConfigurer;
    @Mock
    private EvictionDetailsService evictionDetailsService;
    @Mock
    private EvictionScheduleService evictionScheduleService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new ScheduleEviction(
            scheduleEvictionConfigurer, evictionDetailsService,
            new EvictionScheduleValidator(Clock.fixed(NOW, UK_ZONE_ID)), new CaseReferenceFormatter(),
            evictionScheduleService));
    }

    @Test
    void shouldConfigureThePages() {
        verify(scheduleEvictionConfigurer).configurePages(any());
    }

    @Test
    void shouldOnlyBeOfferedWhenTheWarrantIsAwaitingSchedulingAndEnforcementIsEnabled() {
        assertConfiguredShowConditions("featureFlags.enforcementEnabled=\"YES\" AND canScheduleEviction=\"Yes\"");
    }

    @Test
    void shouldBeAvailableInTheSameStatesAsAddABailiffNote() {
        assertConfiguredForStates(AddBailiffNote.STATES);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "BAILIFF", "BAILIFF_ADMIN", "BAILIFF_MANAGER", "HEARING_CENTRE_ADMIN", "HEARING_CENTRE_TEAM_LEADER"})
    void shouldGrantCrudToBailiffStaff(UserRole userRole) {
        assertGrants(userRole, Permission.CRUD);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "PCS_SOLICITOR", "GA_CLAIMANT_SOLICITOR", "CLAIMANT", "CLAIMANT_SOLICITOR", "DEFENDANT_SOLICITOR",
        "GA_DEFENDANT_SOLICITOR", "DEFENDANT", "CITIZEN", "JUDGE"})
    void shouldNotLetPartiesOrJudgesSchedule(UserRole userRole) {
        assertThat(configuredEvent.getGrants().get(userRole)).doesNotContain(Permission.C, Permission.U);
    }

    @Test
    void shouldGrantHistoryOnlyAccessToJudicialHistoryRoles() {
        for (UserRole role : JUDICIAL_HISTORY_ROLES) {
            assertThat(configuredEvent.getGrants().get(role)).containsExactly(Permission.R);
        }
    }

    @Test
    void shouldShowCheckYourAnswers() {
        assertThat(configuredEvent.isShowSummary()).isTrue();
        assertThat(configuredEvent.getName()).isEqualTo("Schedule eviction");
    }

    @Test
    void shouldRefuseToStartWithoutALiveWarrant() {
        doThrow(new NoEnforcementException()).when(evictionScheduleService).checkCanSchedule(TEST_CASE_REFERENCE);

        assertThatThrownBy(() -> callStartHandler(PCSCase.builder().build()))
            .isInstanceOf(NoEnforcementException.class)
            .hasMessage("There is no warrant on this case");
    }

    @Test
    void shouldStartWithTheEvictionDetailsAndDispenseWithServiceDefaultingToNo() {
        when(evictionDetailsService.getEvictionDetails(TEST_CASE_REFERENCE))
            .thenReturn(new EvictionDetails("Private landlord", List.of("Acme"), List.of("Person(s) unknown")));
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .evictionSchedule(EvictionSchedule.builder()
                                                        .evictionDateTime(LocalDateTime.parse("2026-01-01T10:00"))
                                                        .build())
                                  .build())
            .build();

        PCSCase result = callStartHandler(caseData);

        EvictionSchedule schedule = result.getEnforcementOrder().getEvictionSchedule();
        assertThat(schedule.getEvictionDispenseWithService()).isEqualTo(YesOrNo.NO);
        assertThat(schedule.getEvictionDateTime()).isNull();
        assertThat(schedule.getEvictionDetailsMarkup()).contains("Private landlord", "Acme", "Person(s) unknown");
    }

    @Test
    void shouldStartWithTheCaseNameForThePartiesLine() {
        when(evictionDetailsService.getEvictionDetails(TEST_CASE_REFERENCE))
            .thenReturn(new EvictionDetails("Private landlord", List.of("Acme"), List.of("John Doe")));
        PCSCase caseData = PCSCase.builder().caseNameHmctsInternal("Acme vs John Doe").build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getEnforcementOrder().getFormattedCaseName()).isEqualTo("Acme vs John Doe");
    }

    @Test
    void shouldScheduleAndConfirm() {
        when(evictionScheduleService.schedule(anyLong(), any(), anyBoolean()))
            .thenReturn(EnforcementChange.made("WP-00000042"));

        SubmitResponse<State> response = callSubmitHandler(caseWith(LocalDateTime.parse("2026-06-25T10:00:30"),
                                                                     YesOrNo.NO));

        verify(evictionScheduleService).schedule(TEST_CASE_REFERENCE, LocalDateTime.parse("2026-06-25T10:00"), false);
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getConfirmationHeader())
            .isEqualTo("# Eviction date scheduled for Thursday 25 June 2026 at 10:00am");
        assertThat(response.getConfirmationBody())
            .contains("Case number: 0000-0000-0000-1234", "Warrant number: WP-00000042",
                      "Print the 'notice of eviction' (form N54)");
    }

    @Test
    void shouldLeaveTheWarrantNumberOutOfTheConfirmationWhenTheWarrantHasNone() {
        when(evictionScheduleService.schedule(anyLong(), any(), anyBoolean()))
            .thenReturn(EnforcementChange.made(null));

        SubmitResponse<State> response = callSubmitHandler(caseWith(LocalDateTime.parse("2026-06-25T10:00"),
                                                                     YesOrNo.NO));

        assertThat(response.getConfirmationBody())
            .contains("Case number: 0000-0000-0000-1234")
            .doesNotContain("Warrant number");
    }

    @Test
    void shouldPassOnThatServiceIsDispensedWith() {
        when(evictionScheduleService.schedule(anyLong(), any(), anyBoolean()))
            .thenReturn(EnforcementChange.made("WP-00000042"));

        callSubmitHandler(caseWith(LocalDateTime.parse("2026-06-25T10:00"), YesOrNo.YES));

        verify(evictionScheduleService).schedule(TEST_CASE_REFERENCE, LocalDateTime.parse("2026-06-25T10:00"), true);
    }

    @Test
    void shouldRejectADateInThePast() {
        SubmitResponse<State> response = callSubmitHandler(caseWith(LocalDateTime.parse("2026-06-19T10:00"),
                                                                     YesOrNo.NO));

        assertThat(response.getErrors()).containsExactly("The eviction date must be today or in the future");
        verify(evictionScheduleService, never()).schedule(anyLong(), any(), anyBoolean());
    }

    @Test
    void shouldRejectMissingAnswers() {
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).hasSize(2);
    }

    @Test
    void shouldReturnAnErrorWhenTheWarrantHasMovedOn() {
        when(evictionScheduleService.schedule(anyLong(), any(), anyBoolean()))
            .thenReturn(EnforcementChange.refused("Not now"));

        SubmitResponse<State> response = callSubmitHandler(caseWith(LocalDateTime.parse("2026-06-25T10:00"),
                                                                     YesOrNo.NO));

        assertThat(response.getErrors()).containsExactly("Not now");
        assertThat(response.getConfirmationHeader()).isNull();
    }

    private static PCSCase caseWith(LocalDateTime evictionDateTime, YesOrNo dispense) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .evictionSchedule(EvictionSchedule.builder()
                                                        .evictionDateTime(evictionDateTime)
                                                        .evictionDispenseWithService(dispense)
                                                        .build())
                                  .build())
            .build();
    }
}
