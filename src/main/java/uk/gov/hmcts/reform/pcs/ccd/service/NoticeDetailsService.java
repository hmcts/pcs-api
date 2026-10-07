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

@Slf4j
@Service
@AllArgsConstructor
public class NoticeDetailsService {

    private static final String FUTURE_DATETIME_ERROR = "The date and time cannot be today or in the future";
    private static final String FUTURE_DATE_ERROR = "The date cannot be today or in the future";
    private static final String NOTICE_OTHER_ELECTRONIC_METHOD_EXPLANATION_LABEL =
            "Give details of how the notice was served";
    private static final String NOTICE_UNABLE_TO_UPLOAD_DOCUMENT_TXT =
            "Why can you not upload a copy of the notice you served?";
    private static final String NOTICE_OTHER_EXPLANATION_LABEL = "Other";
    private static final String NAME_OF_PERSON_DOCUMENT_LEFT_WITH_LABEL = "Name of person the document was left with";
    private static final String EMAIL_ADDRESS_LABEL = "What email address was the document sent to?";

    private final TextAreaValidationService textAreaValidationService;

    public List<String> validateNoticeDetails(PCSCase caseData) {
        List<String> errors = new ArrayList<>();

        NoticeServedDetails noticeServedDetails = caseData.getNoticeServedDetails();
        NoticeServiceMethod noticeServiceMethod = caseData.getNoticeServedDetails().getServiceMethod();

        switch (noticeServiceMethod) {
            case FIRST_CLASS_POST:
                validateDateField(noticeServedDetails.getPostedDate(), errors);
                break;
            case DELIVERED_PERMITTED_PLACE:
                validateDateField(noticeServedDetails.getDeliveredDate(), errors);
                break;
            case PERSONALLY_HANDED:
                validateDateTimeField(noticeServedDetails.getHandedOverDateTime(), errors);
                break;
            case EMAIL:
                validateEmail(noticeServedDetails, errors);
                break;
            case OTHER_ELECTRONIC:
                validateDateTimeField(noticeServedDetails.getOtherElectronicDateTime(), errors);
                break;
            case OTHER:
                validateOther(noticeServedDetails, errors);
                break;
        }

        errors.addAll(textAreaValidationService.validateMultipleTextAreas(
                TextAreaValidationService.FieldValidation.of(
                        noticeServedDetails.getOtherExplanation(),
                        NOTICE_OTHER_EXPLANATION_LABEL,
                        TextAreaValidationService.SHORT_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                        noticeServedDetails.getPersonName(),
                        NAME_OF_PERSON_DOCUMENT_LEFT_WITH_LABEL,
                        TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                        noticeServedDetails.getEmailAddress(),
                        EMAIL_ADDRESS_LABEL,
                        TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                        noticeServedDetails.getOtherElectronicExplanation(),
                        NOTICE_OTHER_ELECTRONIC_METHOD_EXPLANATION_LABEL,
                        TextAreaValidationService.SHORT_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                        noticeServedDetails.getUnableToUploadReason(),
                        NOTICE_UNABLE_TO_UPLOAD_DOCUMENT_TXT,
                        TextAreaValidationService.MEDIUM_TEXT_LIMIT
                )
        ));

        return errors;
    }

    private void validateDateField(LocalDate dateValue, List<String> errors) {
        if (dateValue != null && isTodayOrFutureDate(dateValue)) {
            errors.add(FUTURE_DATE_ERROR);
        }
    }

    private void validateDateTimeField(LocalDateTime dateTimeValue, List<String> errors) {
        if (dateTimeValue != null && isTodayOrFutureDateTime(dateTimeValue)) {
            errors.add(FUTURE_DATETIME_ERROR);
        }
    }

    private void validateEmail(NoticeServedDetails noticeServed, List<String> errors) {
        validateDateTimeField(noticeServed.getEmailSentDateTime(), errors);
    }

    private void validateOther(NoticeServedDetails noticeServed, List<String> errors) {
        validateDateTimeField(noticeServed.getOtherDateTime(), errors);
    }

    private boolean isTodayOrFutureDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        return date.isEqual(today) || date.isAfter(today);
    }

    private boolean isTodayOrFutureDateTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        return dateTime.toLocalDate().isEqual(now.toLocalDate()) || dateTime.isAfter(now);
    }
}
