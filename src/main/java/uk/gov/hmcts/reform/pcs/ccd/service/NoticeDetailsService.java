package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private static final String FUTURE_DATE_ERROR = "The date cannot be today or in the future";
    private static final String EXPLANATION_TOO_LONG_ERROR = "The explanation must be 250 characters or fewer";
    private static final String NOTICE_SERVICE_METHOD_REQUIRED = "You must select how you served the notice";

    /**
     * Validates notice details and returns any validation errors.
     * @param caseData the case data containing notice details
     * @return list of error messages, empty if no errors
     */
    public List<String> validateNoticeDetails(PCSCase caseData) {
        List<String> errors = new ArrayList<>();

        if (caseData.getNoticeServed() != null && !caseData.getNoticeServed().toBoolean()) {
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
                validateDateField(caseData.getNoticePostedDate(), errors);
                break;
            case DELIVERED_PERMITTED_PLACE:
                validateDateField(caseData.getNoticeDeliveredDate(), errors);
                break;
            case PERSONALLY_HANDED:
                validateDateTimeField(caseData.getNoticeHandedOverDateTime(), errors);
                break;
            case EMAIL:
                validateEmail(caseData, errors);
                break;
            case OTHER_ELECTRONIC:
                validateDateTimeField(caseData.getNoticeOtherElectronicDateTime(), errors);
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
    private void validateDateField(LocalDate dateValue, List<String> errors) {
        if (dateValue != null && isTodayOrFutureDate(dateValue)) {
            errors.add(FUTURE_DATE_ERROR);
        }
    }

    /**
     * Validates a datetime field with common validation logic.
     */
    private void validateDateTimeField(LocalDateTime dateTimeValue, List<String> errors) {
        if (dateTimeValue != null) {
            if (!isValidLocalDateTime(dateTimeValue)) {
                errors.add(INVALID_DATETIME_ERROR);
            } else if (isTodayOrFutureDateTime(dateTimeValue)) {
                errors.add(FUTURE_DATETIME_ERROR);
            }
        }
    }

    /**
     * Validates an explanation field with length validation.
     */
    private void validateExplanationField(String explanation, List<String> errors) {
        if (explanation != null && explanation.length() > 250) {
            errors.add(EXPLANATION_TOO_LONG_ERROR);
        }
    }

    private void validateEmail(PCSCase caseData, List<String> errors) {
        validateExplanationField(caseData.getNoticeEmailExplanation(), errors);
        validateDateTimeField(caseData.getNoticeEmailSentDateTime(), errors);
    }

    private void validateOther(PCSCase caseData, List<String> errors) {
        validateExplanationField(caseData.getNoticeOtherExplanation(), errors);
        validateDateTimeField(caseData.getNoticeOtherDateTime(), errors);
    }


    private boolean isTodayOrFutureDate(LocalDate date) {
        if (date == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        return date.isEqual(today) || date.isAfter(today);
    }

    private boolean isValidLocalDateTime(LocalDateTime dateTime) {
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
