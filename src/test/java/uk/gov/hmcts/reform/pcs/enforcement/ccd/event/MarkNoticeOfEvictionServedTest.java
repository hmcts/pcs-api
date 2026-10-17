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
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.MarkNoticeOfEvictionServedConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementActionNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class MarkNoticeOfEvictionServedTest extends BaseEventTest {

    // 08/07/2026 10:00 in London
    private static final Instant NOW = Instant.parse("2026-07-08T09:00:00Z");

    @Mock
    private MarkNoticeOfEvictionServedConfigurer configurer;
    @Mock
    private EvictionScheduleService evictionScheduleService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new MarkNoticeOfEvictionServed(
            configurer, evictionScheduleService, new EvictionScheduleValidator(Clock.fixed(NOW, UK_ZONE_ID)),
            new CaseReferenceFormatter()));
    }

    @Test
    void shouldBeGatedOnTheNoticeBeingDueAndTheEnforcementFeatureFlag() {
        assertConfiguredShowConditions(
            "featureFlags.enforcementEnabled=\"YES\" AND canMarkNoticeOfEvictionServed=\"Yes\"");
        assertThat(configuredEvent.getName()).isEqualTo("Mark eviction notice served");
        assertThat(configuredEvent.getName()).hasSizeLessThanOrEqualTo(30);
        assertThat(configuredEvent.isShowSummary()).isTrue();
        assertConfiguredForStates(AddBailiffNote.STATES);
        verify(configurer).configurePages(any());
    }

    @Test
    void shouldGrantCrudToBailiffStaff() {
        assertGrants(UserRole.BAILIFF, Permission.CRUD);
        assertGrants(UserRole.HEARING_CENTRE_ADMIN, Permission.CRUD);
        assertThat(configuredEvent.getGrants().get(UserRole.PCS_SOLICITOR)).doesNotContain(Permission.C);
    }

    @Test
    void shouldRefuseToStartWhenTheNoticeIsNotDue() {
        doThrow(new EnforcementActionNotAvailableException(EvictionScheduleService.CANNOT_MARK_NOTICE_SERVED_ERROR))
            .when(evictionScheduleService).checkCanMarkNoticeServed(TEST_CASE_REFERENCE);

        assertThatThrownBy(() -> callStartHandler(PCSCase.builder().build()))
            .isInstanceOf(EnforcementActionNotAvailableException.class);
    }

    @Test
    void shouldStartWithEmptyAnswers() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .bailiffNoteText("left over")
                                  .evictionSchedule(EvictionSchedule.builder()
                                                        .noticeOfEvictionServedHow("left over")
                                                        .build())
                                  .build())
            .build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getEnforcementOrder().getBailiffNoteText()).isNull();
        assertThat(result.getEnforcementOrder().getEvictionSchedule()).isEqualTo(new EvictionSchedule());
    }

    @Test
    void shouldRecordTheServiceAndConfirm() {
        SubmitResponse<State> response = callSubmitHandler(
            caseWith(LocalDateTime.parse("2026-03-15T08:00"), YesOrNo.NO, "ignored"));

        verify(evictionScheduleService).markNoticeServed(
            TEST_CASE_REFERENCE, LocalDateTime.parse("2026-03-15T08:00"), "by hand to the occupant", null);
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getConfirmationHeader()).isEqualTo("# 'Notice of eviction' marked as served");
        assertThat(response.getConfirmationBody()).isEqualTo("Case number: 0000-0000-0000-1234");
    }

    @Test
    void shouldPassOnTheBailiffNote() {
        callSubmitHandler(caseWith(LocalDateTime.parse("2026-03-15T08:00"), YesOrNo.YES,
                                   "Spoke to occupant at the door"));

        verify(evictionScheduleService).markNoticeServed(
            TEST_CASE_REFERENCE, LocalDateTime.parse("2026-03-15T08:00"), "by hand to the occupant",
            "Spoke to occupant at the door");
    }

    @Test
    void shouldRejectAServiceTimeInTheFuture() {
        SubmitResponse<State> response = callSubmitHandler(
            caseWith(LocalDateTime.parse("2026-07-08T16:00"), YesOrNo.NO, null));

        assertThat(response.getErrors())
            .containsExactly("The date and time you served the 'notice of eviction' cannot be in the future");
        verify(evictionScheduleService, never()).markNoticeServed(anyLong(), any(), anyString(), isNull());
    }

    @Test
    void shouldReturnAnErrorWhenTheNoticeIsNoLongerDue() {
        doThrow(new EnforcementActionNotAvailableException("Not now"))
            .when(evictionScheduleService).markNoticeServed(anyLong(), any(), anyString(), isNull());

        SubmitResponse<State> response = callSubmitHandler(
            caseWith(LocalDateTime.parse("2026-03-15T08:00"), YesOrNo.NO, null));

        assertThat(response.getErrors()).containsExactly("Not now");
    }

    private static PCSCase caseWith(LocalDateTime servedAt, YesOrNo addNote, String note) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .bailiffNoteText(note)
                                  .evictionSchedule(EvictionSchedule.builder()
                                                        .noticeOfEvictionServedAt(servedAt)
                                                        .noticeOfEvictionServedHow("by hand to the occupant")
                                                        .noticeOfEvictionAddBailiffNote(addNote)
                                                        .build())
                                  .build())
            .build();
    }
}
