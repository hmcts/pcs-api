package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator.ADD_BAILIFF_NOTE_REQUIRED_ERROR;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator.BAILIFF_NOTE_REQUIRED_ERROR;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator.BAILIFF_NOTE_TOO_LONG_ERROR;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator.DISPENSE_WITH_SERVICE_REQUIRED_ERROR;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator.EVICTION_DATE_IN_PAST_ERROR;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator.EVICTION_DATE_REQUIRED_ERROR;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator.SERVED_AT_IN_FUTURE_ERROR;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator.SERVED_AT_REQUIRED_ERROR;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator.SERVED_HOW_REQUIRED_ERROR;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator.SERVED_HOW_TOO_LONG_ERROR;

class EvictionScheduleValidatorTest {

    // 08/07/2026 10:00:30 in London (BST), as in 08-mark-notice-of-eviction-served.feature
    private static final Instant NOW = Instant.parse("2026-07-08T09:00:30Z");

    private final EvictionScheduleValidator underTest =
        new EvictionScheduleValidator(Clock.fixed(NOW, UK_ZONE_ID));

    @Test
    void shouldAcceptAnEvictionLaterToday() {
        assertThat(underTest.validateEvictionDate(schedule(LocalDateTime.parse("2026-07-08T16:00"))))
            .isEmpty();
    }

    @Test
    void shouldAcceptAnEvictionEarlierToday() {
        assertThat(underTest.validateEvictionDate(schedule(LocalDateTime.parse("2026-07-08T00:00"))))
            .isEmpty();
    }

    @Test
    void shouldAcceptAnEvictionInTheFuture() {
        assertThat(underTest.validateEvictionDate(schedule(LocalDateTime.parse("2026-07-09T10:00"))))
            .isEmpty();
    }

    @Test
    void shouldRejectAnEvictionYesterday() {
        assertThat(underTest.validateEvictionDate(schedule(LocalDateTime.parse("2026-07-07T23:59"))))
            .containsExactly(EVICTION_DATE_IN_PAST_ERROR);
    }

    @Test
    void shouldUseTheUkDateNotTheUtcDate() {
        // 00:30 on 9 July in London is still 8 July in UTC
        EvictionScheduleValidator justAfterUkMidnight =
            new EvictionScheduleValidator(Clock.fixed(Instant.parse("2026-07-08T23:30:00Z"), UK_ZONE_ID));

        assertThat(justAfterUkMidnight.validateEvictionDate(schedule(LocalDateTime.parse("2026-07-08T12:00"))))
            .containsExactly(EVICTION_DATE_IN_PAST_ERROR);
    }

    @Test
    void shouldRequireTheEvictionDateAndDispenseWithServiceAnswer() {
        assertThat(underTest.validateEvictionDate(new EvictionSchedule()))
            .containsExactly(EVICTION_DATE_REQUIRED_ERROR, DISPENSE_WITH_SERVICE_REQUIRED_ERROR);
        assertThat(underTest.validateEvictionDate(null))
            .containsExactly(EVICTION_DATE_REQUIRED_ERROR, DISPENSE_WITH_SERVICE_REQUIRED_ERROR);
    }

    @Test
    void shouldAcceptANoticeServedYesterdayAfternoon() {
        assertThat(underTest.validateNoticeServed(served(LocalDateTime.parse("2026-07-07T16:00")), null))
            .isEmpty();
    }

    @Test
    void shouldAcceptANoticeServedEarlierToday() {
        assertThat(underTest.validateNoticeServed(served(LocalDateTime.parse("2026-07-08T09:00")), null))
            .isEmpty();
    }

    @Test
    void shouldAcceptANoticeServedThisMinute() {
        assertThat(underTest.validateNoticeServed(served(LocalDateTime.parse("2026-07-08T10:00:59")), null))
            .isEmpty();
    }

    @Test
    void shouldRejectANoticeServedLaterToday() {
        assertThat(underTest.validateNoticeServed(served(LocalDateTime.parse("2026-07-08T16:00")), null))
            .containsExactly(SERVED_AT_IN_FUTURE_ERROR);
    }

    @Test
    void shouldRejectANoticeServedNextMinute() {
        assertThat(underTest.validateNoticeServed(served(LocalDateTime.parse("2026-07-08T10:01")), null))
            .containsExactly(SERVED_AT_IN_FUTURE_ERROR);
    }

    @Test
    void shouldRequireWhenAndHowTheNoticeWasServed() {
        assertThat(underTest.validateNoticeServed(new EvictionSchedule(), null))
            .containsExactly(SERVED_AT_REQUIRED_ERROR, SERVED_HOW_REQUIRED_ERROR, ADD_BAILIFF_NOTE_REQUIRED_ERROR);
        assertThat(underTest.validateNoticeServed(null, null))
            .containsExactly(SERVED_AT_REQUIRED_ERROR, SERVED_HOW_REQUIRED_ERROR, ADD_BAILIFF_NOTE_REQUIRED_ERROR);
    }

    @Test
    void shouldAcceptHowTheNoticeWasServedUpTo500Characters() {
        EvictionSchedule schedule = served(LocalDateTime.parse("2026-07-07T16:00"));
        schedule.setNoticeOfEvictionServedHow("a".repeat(500));

        assertThat(underTest.validateNoticeServed(schedule, null)).isEmpty();
    }

    @Test
    void shouldRejectHowTheNoticeWasServedOver500Characters() {
        EvictionSchedule schedule = served(LocalDateTime.parse("2026-07-07T16:00"));
        schedule.setNoticeOfEvictionServedHow("a".repeat(501));

        assertThat(underTest.validateNoticeServed(schedule, null)).containsExactly(SERVED_HOW_TOO_LONG_ERROR);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void shouldRequireTheBailiffNoteWhenAddingOne(String note) {
        EvictionSchedule schedule = served(LocalDateTime.parse("2026-07-07T16:00"));
        schedule.setNoticeOfEvictionAddBailiffNote(YesOrNo.YES);

        assertThat(underTest.validateNoticeServed(schedule, note)).containsExactly(BAILIFF_NOTE_REQUIRED_ERROR);
    }

    @Test
    void shouldLimitTheBailiffNoteTo6800Characters() {
        EvictionSchedule schedule = served(LocalDateTime.parse("2026-07-07T16:00"));
        schedule.setNoticeOfEvictionAddBailiffNote(YesOrNo.YES);

        assertThat(underTest.validateNoticeServed(schedule, "a".repeat(6800))).isEmpty();
        assertThat(underTest.validateNoticeServed(schedule, "a".repeat(6801)))
            .containsExactly(BAILIFF_NOTE_TOO_LONG_ERROR);
    }

    @Test
    void shouldIgnoreTheBailiffNoteWhenNotAddingOne() {
        assertThat(underTest.validateNoticeServed(served(LocalDateTime.parse("2026-07-07T16:00")), "a".repeat(6801)))
            .isEmpty();
    }

    private static EvictionSchedule schedule(LocalDateTime evictionDateTime) {
        return EvictionSchedule.builder()
            .evictionDateTime(evictionDateTime)
            .evictionDispenseWithService(YesOrNo.NO)
            .build();
    }

    private static EvictionSchedule served(LocalDateTime servedAt) {
        return EvictionSchedule.builder()
            .noticeOfEvictionServedAt(servedAt)
            .noticeOfEvictionServedHow("by hand to the occupant")
            .noticeOfEvictionAddBailiffNote(YesOrNo.NO)
            .build();
    }
}
