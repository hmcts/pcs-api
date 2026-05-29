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

    private final TextAreaValidationService textAreaValidationService;

    private static final String FUTURE_DATETIME_ERROR = "The date and time cannot be today or in the future";
    private static final String FUTURE_DATE_ERROR = "The date cannot be today or in the future";
    private static final String NOTICE_SERVICE_METHOD_REQUIRED = "You must select how you served the notice";
    private static final String NOTICE_OTHER_ELECTRONIC_METHOD_EXPLANATION_LABEL =
        "Give details of how the notice was served";
    private static final String NOTICE_UNABLE_TO_UPLOAD_DOCUMENT_TXT =
        "Why can you not upload a copy of the notice you served?";
    private static final String NOTICE_OTHER_EXPLANATION_LABEL = "Other";
    private static final String NAME_OF_PERSON_DOCUMENT_LEFT_WITH = "Name of person the document was left with";

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
            default:
                break;
        }

        errors.addAll(textAreaValidationService.validateMultipleTextAreas(
            TextAreaValidationService.FieldValidation.of(
                noticeServedDetails.getNoticeOtherExplanation(),
                NOTICE_OTHER_EXPLANATION_LABEL,
                TextAreaValidationService.SHORT_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                noticeServedDetails.getNoticePersonName(),
                NAME_OF_PERSON_DOCUMENT_LEFT_WITH,
                TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                noticeServedDetails.getNoticeOtherElectronicMethodExplanation(),
                NOTICE_OTHER_ELECTRONIC_METHOD_EXPLANATION_LABEL,
                TextAreaValidationService.SHORT_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                noticeServedDetails.getUnableToUploadTxt(),
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
        validateDateTimeField(noticeServed.getNoticeEmailSentDateTime(), errors);
    }

    private void validateOther(NoticeServedDetails noticeServed, List<String> errors) {
        validateDateTimeField(noticeServed.getNoticeOtherDateTime(), errors);
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
