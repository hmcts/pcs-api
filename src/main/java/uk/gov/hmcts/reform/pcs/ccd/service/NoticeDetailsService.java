package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling notice details validation.
 */
@Slf4j
@Service
@AllArgsConstructor
public class NoticeDetailsService {
    
    // Error message constants
    private static final String INVALID_DATETIME_ERROR = "Enter a valid date and time in the format DD MM YYYY HH MM";
    private static final String FUTURE_DATETIME_ERROR = "The date and time cannot be today or in the future";
    private static final String INVALID_DATE_ERROR = "Enter a valid date in the format YYYY-MM-DD";
    private static final String FUTURE_DATE_ERROR = "The date cannot be today or in the future";
    private static final String EXPLANATION_TOO_LONG_ERROR = "The explanation must be 250 characters or fewer";
    private static final String NOTICE_SERVICE_METHOD_REQUIRED = "You must select how you served the notice";
    
    // Field name constants
    private static final String NOTICE_POSTED_DATE = "noticePostedDate";
    private static final String NOTICE_DELIVERED_DATE = "noticeDeliveredDate";
    private static final String NOTICE_HANDED_OVER_DATETIME = "noticeHandedOverDateTime";
    private static final String NOTICE_EMAIL_EXPLANATION = "noticeEmailExplanation";
    private static final String NOTICE_EMAIL_SENT_DATETIME = "noticeEmailSentDateTime";
    private static final String NOTICE_OTHER_ELECTRONIC_DATETIME = "noticeOtherElectronicDateTime";
    private static final String NOTICE_OTHER_EXPLANATION = "noticeOtherExplanation";
    private static final String NOTICE_OTHER_DATETIME = "noticeOtherDateTime";

    /**
     * Validates notice details and returns any validation errors.
     * @param caseData the case data containing notice details
     * @return list of error messages, empty if no errors
     */
    public List<String> validateNoticeDetails(PCSCase caseData) {
        List<String> errors = new ArrayList<>();

        if (caseData.getNoticeServed() == null || !caseData.getNoticeServed().toBoolean()) {
            return errors;
        }

        NoticeServiceMethod noticeServiceMethod = caseData.getNoticeServiceMethod();
        if (noticeServiceMethod == null) {
            errors.add(NOTICE_SERVICE_METHOD_REQUIRED);
            return errors;
        }

        // Validate based on selected method
        switch (noticeServiceMethod) {
            case FIRST_CLASS_POST:
                validateDateField(caseData.getNoticePostedDate(), NOTICE_POSTED_DATE, errors);
                break;
            case DELIVERED_PERMITTED_PLACE:
                validateDateField(caseData.getNoticeDeliveredDate(), NOTICE_DELIVERED_DATE, errors);
                break;
            case PERSONALLY_HANDED:
                validateDateTimeField(caseData.getNoticeHandedOverDateTime(), NOTICE_HANDED_OVER_DATETIME, errors);
                break;
            case EMAIL:
                validateEmail(caseData, errors);
                break;
            case OTHER_ELECTRONIC:
                validateDateTimeField(caseData.getNoticeOtherElectronicDateTime(), 
                                   NOTICE_OTHER_ELECTRONIC_DATETIME, errors);
                break;
            case OTHER:
                validateOther(caseData, errors);
                break;
        }

        return errors;
    }

    /**
     * Validates a date field with common validation logic.
     */
    private void validateDateField(LocalDate dateValue, String fieldName, List<String> errors) {
        if (dateValue != null) {
            if (isFutureDate(dateValue)) {
                errors.add(FUTURE_DATE_ERROR);
            }
        }
    }

    /**
     * Validates a datetime field with common validation logic.
     */
    private void validateDateTimeField(LocalDateTime dateTimeValue, String fieldName, List<String> errors) {
        if (dateTimeValue != null) {
            if (!isValidDateTime(dateTimeValue)) {
                errors.add(INVALID_DATETIME_ERROR);
            } else if (isTodayOrFutureDateTime(dateTimeValue)) {
                errors.add(FUTURE_DATETIME_ERROR);
            }
        }
    }

    /**
     * Validates an explanation field with length validation.
     */
    private void validateExplanationField(String explanation, String fieldName, List<String> errors) {
        if (explanation != null && explanation.length() > 250) {
            errors.add(EXPLANATION_TOO_LONG_ERROR);
        }
    }

    private void validateEmail(PCSCase caseData, List<String> errors) {
        validateExplanationField(caseData.getNoticeEmailExplanation(), NOTICE_EMAIL_EXPLANATION, errors);
        validateDateTimeField(caseData.getNoticeEmailSentDateTime(), NOTICE_EMAIL_SENT_DATETIME, errors);
    }

    private void validateOther(PCSCase caseData, List<String> errors) {
        validateExplanationField(caseData.getNoticeOtherExplanation(), NOTICE_OTHER_EXPLANATION, errors);
        validateDateTimeField(caseData.getNoticeOtherDateTime(), NOTICE_OTHER_DATETIME, errors);
    }

    /**
     * Parses a date string and returns a LocalDate, or null if invalid.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        
        try {
            String trimmed = dateStr.trim();
            
            // Match ISO format (YYYY-MM-DD)
            if (trimmed.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                return LocalDate.parse(trimmed);
            }
            
            return null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private boolean isValidDate(String dateStr) {
        return parseDate(dateStr) != null;
    }

    private boolean isFutureDate(LocalDate date) {
        if (date == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        return date.isEqual(today) || date.isAfter(today);
    }

    private boolean isValidDateTime(LocalDateTime dateTime) {
        try {
            // LocalDateTime is already validated by the framework, just check if it's not null
            return dateTime != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTodayOrFutureDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        return dateTime.toLocalDate().isEqual(now.toLocalDate()) || dateTime.isAfter(now);
    }
}
