package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
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

    private final TextValidationService textValidationService;

    // Error message constants
    private static final String INVALID_DATETIME_ERROR = "Enter a valid date and time in the format DD MM YYYY HH MM";
    private static final String FUTURE_DATETIME_ERROR = "The date and time cannot be today or in the future";
    private static final String FUTURE_DATE_ERROR = "The date cannot be today or in the future";
    private static final String NOTICE_SERVICE_METHOD_REQUIRED = "You must select how you served the notice";
    private static final String NOTICE_EMAIL_EXPLANATION_LABEL = "Explain how it was served by email";
    private static final String NOTICE_OTHER_EXPLANATION_LABEL = "Explain what the other means were";


    /**
     * Validates notice details and returns any validation errors.
     * @param caseData the case data containing notice details
     * @return list of error messages, empty if no errors
     */
    public List<String> validateNoticeDetails(PCSCase caseData) {
        List<String> errors = new ArrayList<>();

        if (caseData.getNoticeServedDetails() == null
            || (caseData.getNoticeServed() != null
                && !caseData.getNoticeServed().toBoolean())) {
            return errors;
        }

        NoticeServedDetails noticeServedDetails = caseData.getNoticeServedDetails();
        NoticeServiceMethod noticeServiceMethod = caseData.getNoticeServedDetails().getNoticeServiceMethod();
        if (noticeServiceMethod == null) {
            errors.add(NOTICE_SERVICE_METHOD_REQUIRED);
            return errors;
        }

        // Validate based on selected method
        switch (noticeServiceMethod) {
            case FIRST_CLASS_POST:
                validateDateField(noticeServedDetails.getNoticePostedDate(), errors);
                break;
            case DELIVERED_PERMITTED_PLACE:
                validateDateField(noticeServedDetails.getNoticeDeliveredDate(), errors);
                break;
            case PERSONALLY_HANDED:
                validateDateTimeField(noticeServedDetails.getNoticeHandedOverDateTime(), errors);
                break;
            case EMAIL:
                validateEmail(noticeServedDetails, errors);
                break;
            case OTHER_ELECTRONIC:
                validateDateTimeField(noticeServedDetails.getNoticeOtherElectronicDateTime(), errors);
                break;
            case OTHER:
                validateOther(noticeServedDetails, errors);
                break;
        }

        // Validate textarea fields for character limits
        errors.addAll(textValidationService.validateMultipleTextAreas(
            TextValidationService.FieldValidation.of(
                noticeServedDetails.getNoticeEmailExplanation(),
                NOTICE_EMAIL_EXPLANATION_LABEL,
                TextValidationService.SHORT_TEXT_LIMIT
            ),
            TextValidationService.FieldValidation.of(
                noticeServedDetails.getNoticeOtherExplanation(),
                NOTICE_OTHER_EXPLANATION_LABEL,
                TextValidationService.SHORT_TEXT_LIMIT
            )
        ));

        return errors;
    }

    /**
     * Validates a date field with common validation logic.
     */
    private void validateDateField(LocalDate dateValue, List<String> errors) {
        if (isTodayOrFutureDate(dateValue)) {
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

    private void validateEmail(NoticeServedDetails noticeServed, List<String> errors) {
        validateDateTimeField(noticeServed.getNoticeEmailSentDateTime(), errors);
    }

    private void validateOther(NoticeServedDetails noticeServed, List<String> errors) {
        validateDateTimeField(noticeServed.getNoticeOtherDateTime(), errors);
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
