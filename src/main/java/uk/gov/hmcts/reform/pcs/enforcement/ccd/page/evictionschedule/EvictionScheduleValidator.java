package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule.NOTICE_SERVED_HOW_MAX_LENGTH;

/**
 * Validates the answers to the schedule, reschedule and notice of eviction events. XUI already rejects a date or
 * time that does not exist, such as 31/02 or 25:00. Times are UK wall-clock times.
 */
@Component
public class EvictionScheduleValidator {

    public static final String EVICTION_DATE_REQUIRED_ERROR = "Enter the eviction date and time";
    public static final String EVICTION_DATE_IN_PAST_ERROR = "The eviction date must be today or in the future";
    public static final String DISPENSE_WITH_SERVICE_REQUIRED_ERROR =
        "Select if the court has given permission to dispense with service";
    public static final String SERVED_AT_REQUIRED_ERROR = "Enter when you served the 'notice of eviction'";
    public static final String SERVED_AT_IN_FUTURE_ERROR =
        "The date and time you served the 'notice of eviction' cannot be in the future";
    public static final String SERVED_HOW_REQUIRED_ERROR = "Enter how you served the 'notice of eviction'";
    public static final String SERVED_HOW_TOO_LONG_ERROR =
        "How you served the 'notice of eviction' must be 500 characters or fewer";
    public static final String ADD_BAILIFF_NOTE_REQUIRED_ERROR = "Select if you want to add a bailiff note";
    public static final String BAILIFF_NOTE_REQUIRED_ERROR = "Enter a note";
    public static final String BAILIFF_NOTE_TOO_LONG_ERROR = "The note must be 6,800 characters or fewer";

    private final Clock ukClock;

    public EvictionScheduleValidator(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    /**
     * The eviction can be today, at any time, or on a later day.
     */
    public List<String> validateEvictionDate(EvictionSchedule schedule) {
        List<String> errors = new ArrayList<>();
        LocalDateTime evictionDateTime = schedule == null ? null : schedule.getEvictionDateTime();
        if (evictionDateTime == null) {
            errors.add(EVICTION_DATE_REQUIRED_ERROR);
        } else if (evictionDateTime.toLocalDate().isBefore(LocalDate.now(ukClock))) {
            errors.add(EVICTION_DATE_IN_PAST_ERROR);
        }
        if (schedule == null || schedule.getEvictionDispenseWithService() == null) {
            errors.add(DISPENSE_WITH_SERVICE_REQUIRED_ERROR);
        }
        return errors;
    }

    /**
     * The notice can have been served now or earlier, to the minute, but not later.
     */
    public List<String> validateNoticeServed(EvictionSchedule schedule, String bailiffNote) {
        List<String> errors = new ArrayList<>();
        LocalDateTime servedAt = schedule == null ? null : schedule.getNoticeOfEvictionServedAt();
        if (servedAt == null) {
            errors.add(SERVED_AT_REQUIRED_ERROR);
        } else if (servedAt.truncatedTo(ChronoUnit.MINUTES)
            .isAfter(LocalDateTime.now(ukClock).truncatedTo(ChronoUnit.MINUTES))) {
            errors.add(SERVED_AT_IN_FUTURE_ERROR);
        }

        String servedHow = schedule == null ? null : schedule.getNoticeOfEvictionServedHow();
        if (servedHow == null || servedHow.isBlank()) {
            errors.add(SERVED_HOW_REQUIRED_ERROR);
        } else if (servedHow.length() > NOTICE_SERVED_HOW_MAX_LENGTH) {
            errors.add(SERVED_HOW_TOO_LONG_ERROR);
        }

        YesOrNo addBailiffNote = schedule == null ? null : schedule.getNoticeOfEvictionAddBailiffNote();
        if (addBailiffNote == null) {
            errors.add(ADD_BAILIFF_NOTE_REQUIRED_ERROR);
        } else if (addBailiffNote == YesOrNo.YES) {
            if (bailiffNote == null || bailiffNote.isBlank()) {
                errors.add(BAILIFF_NOTE_REQUIRED_ERROR);
            } else if (bailiffNote.length() > RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT) {
                errors.add(BAILIFF_NOTE_TOO_LONG_ERROR);
            }
        }
        return errors;
    }
}
