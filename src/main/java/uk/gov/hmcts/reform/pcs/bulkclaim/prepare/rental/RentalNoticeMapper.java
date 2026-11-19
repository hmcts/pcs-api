package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkLimits;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.NoticeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CanUploadNoticeServedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector.NOT_IN_PAST;

/**
 * Maps the England notice of possession. A notice document is never supplied, so the claim records why it
 * cannot be uploaded.
 */
@Component
public class RentalNoticeMapper {

    private final BulkClaimProperties properties;
    private final Clock ukClock;

    public RentalNoticeMapper(BulkClaimProperties properties, @Qualifier("ukClock") Clock ukClock) {
        this.properties = properties;
        this.ukClock = ukClock;
    }

    /**
     * Maps the notice of possession onto the case.
     *
     * @param path path of the Notice element relative to the claim
     * @return the free-text notice type for the claim extras, or null
     */
    public String apply(NoticeType notice, PCSCase target, String path, BulkErrorCollector errors) {
        if (notice == null) {
            errors.add(path, BulkErrorCollector.REQUIRED, "Notice is required for a claim in England");
            return null;
        }

        errors.maxLength(path + "/NoticeType", notice.getNoticeType(), BulkLimits.SIGNATORY, "Notice type");

        if (!notice.isServed()) {
            target.setNoticeServed(YesOrNo.NO);
            return notice.getNoticeType();
        }

        target.setNoticeServed(YesOrNo.YES);
        target.setNoticeServedDetails(servedDetails(notice, path, errors));
        return notice.getNoticeType();
    }

    /**
     * Maps how and when a served notice was served.
     *
     * @param path path of the notice element relative to the claim
     */
    public NoticeServedDetails servedDetails(NoticeType notice, String path, BulkErrorCollector errors) {
        NoticeServedDetails.NoticeServedDetailsBuilder builder = NoticeServedDetails.builder()
            .ableToUploadDocument(CanUploadNoticeServedDocument.No)
            .unableToUploadReason(properties.getSystemReasonNoDocuments());

        if (notice.getServiceMethod() == null) {
            errors.add(path + "/ServiceMethod", BulkErrorCollector.REQUIRED,
                       "The method of service is required when the notice was served");
            return builder.build();
        }

        NoticeServiceMethod method = NoticeServiceMethod.valueOf(notice.getServiceMethod().name());
        builder.serviceMethod(method);
        switch (method) {
            case FIRST_CLASS_POST -> builder.postedDate(date(notice, path, errors));
            case DELIVERED_PERMITTED_PLACE -> builder.deliveredDate(date(notice, path, errors));
            case PERSONALLY_HANDED -> {
                builder.handedOverDateTime(dateTime(notice, path, errors));
                errors.requiredWithMaxLength(path + "/PersonName", notice.getPersonName(), BulkLimits.NAME,
                                             "Name of person the document was left with");
                builder.personName(notice.getPersonName());
            }
            case EMAIL -> {
                builder.emailSentDateTime(dateTime(notice, path, errors));
                errors.requiredWithMaxLength(path + "/EmailAddress", notice.getEmailAddress(), BulkLimits.EMAIL,
                                             "Email address the notice was sent to");
                builder.emailAddress(notice.getEmailAddress());
            }
            case OTHER_ELECTRONIC -> {
                builder.otherElectronicDateTime(dateTime(notice, path, errors));
                errors.requiredWithMaxLength(path + "/Explanation", notice.getExplanation(), BulkLimits.SHORT_TEXT,
                                             PCSCase.NOTICE_OTHER_EXPLANATION_LABEL);
                builder.otherElectronicExplanation(notice.getExplanation());
            }
            case OTHER -> {
                builder.otherDateTime(dateTime(notice, path, errors));
                errors.requiredWithMaxLength(path + "/Explanation", notice.getExplanation(), BulkLimits.SHORT_TEXT,
                                             PCSCase.NOTICE_OTHER_EXPLANATION_LABEL);
                builder.otherExplanation(notice.getExplanation());
            }
        }
        return builder.build();
    }

    private LocalDate date(NoticeType notice, String path, BulkErrorCollector errors) {
        LocalDate date = notice.getServedDate();
        if (errors.required(path + "/ServedDate", date, "Date the notice was served")
            && !date.isBefore(LocalDate.now(ukClock))) {
            errors.add(path + "/ServedDate", NOT_IN_PAST, "The date cannot be today or in the future");
        }
        return date;
    }

    private LocalDateTime dateTime(NoticeType notice, String path, BulkErrorCollector errors) {
        LocalDateTime dateTime = notice.getServedDateTime();
        if (errors.required(path + "/ServedDateTime", dateTime, "Date and time the notice was served")
            && !dateTime.toLocalDate().isBefore(LocalDate.now(ukClock))) {
            errors.add(path + "/ServedDateTime", NOT_IN_PAST, "The date and time cannot be today or in the future");
        }
        return dateTime;
    }
}
