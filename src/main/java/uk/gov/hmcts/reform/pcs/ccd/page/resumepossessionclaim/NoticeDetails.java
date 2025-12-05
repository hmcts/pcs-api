package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.NoticeDetailsService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

/**
 * CCD page configuration for Notice Details.
 * Allows users to specify how they served notice to the defendant.
 */
@Component
@RequiredArgsConstructor
public class NoticeDetails implements CcdPageConfiguration {

    private final NoticeDetailsService noticeDetailsService;
    private final TextAreaValidationService textAreaValidationService;

    private static final String NOTICE_SERVICE_METHOD_CONDITION = "engNoticeServiceMethod=\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("noticeDetails", this::midEvent)
            .pageLabel("Notice details")
            .showCondition("noticeServed=\"Yes\""
                               + " OR walesNoticeServed=\"Yes\"")
            .label("noticeDetails-separator", "---")
            .complex(PCSCase::getNoticeServedDetails)
            .mandatory(NoticeServedDetails::getNoticeServiceMethod)

            // First class post
            .label("noticeDetails-firstClassPost-section", """
                <h3 class="govuk-heading-s">By first class post or other service which provides for
                delivery on the next business day</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.FIRST_CLASS_POST + "\"")
            .optional(
                NoticeServedDetails::getNoticePostedDate,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.FIRST_CLASS_POST + "\""
            )

            // Delivered to permitted place
            .label("noticeDetails-deliveredPermittedPlace-section", """
                <h3 class="govuk-heading-s">By delivering it to or leaving it at a permitted place</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.DELIVERED_PERMITTED_PLACE + "\"")
            .optional(
                NoticeServedDetails::getNoticeDeliveredDate,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.DELIVERED_PERMITTED_PLACE + "\""
            )

            // Personally handed
            .label("noticeDetails-personallyHanded-section", """
                <h3 class="govuk-heading-s">By personally handing it to or leaving it with someone</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.PERSONALLY_HANDED + "\"")
            .optional(
                NoticeServedDetails::getNoticePersonName,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.PERSONALLY_HANDED + "\""
            )
            .optional(
                NoticeServedDetails::getNoticeHandedOverDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.PERSONALLY_HANDED + "\""
            )

            // Email
            .label("noticeDetails-email-section", """
                <h3 class="govuk-heading-s">By email</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.EMAIL + "\"")
            .optional(
                NoticeServedDetails::getNoticeEmailExplanation,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.EMAIL + "\""
            )
            .optional(
                NoticeServedDetails::getNoticeEmailSentDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.EMAIL + "\""
            )

            // Other electronic method
            .label("noticeDetails-otherElectronic-section", """
                <h3 class="govuk-heading-s">By other electronic method</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER_ELECTRONIC + "\"")
            .optional(
                NoticeServedDetails::getNoticeOtherElectronicDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER_ELECTRONIC + "\""
            )

            // Other
            .label("noticeDetails-other-section", """
                <h3 class="govuk-heading-s">Other</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER + "\"")
            .optional(
                NoticeServedDetails::getNoticeOtherExplanation,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER + "\""
            )
            .optional(
                NoticeServedDetails::getNoticeOtherDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER + "\""
            )

            // Document upload section
            .label("noticeDetails-documentUpload-section", """
                ---
                <h2 class="govuk-heading-m">Do you want to upload a copy of the notice you served or the
                certificate of service? (Optional)</h2>
                <p class="govuk-hint">You can either upload this now or closer to the hearing date.
                Any documents you upload now will be included in the pack of documents a judge will
                receive before the hearing (the bundle)</p>
                """)
              .optional(NoticeServedDetails::getNoticeDocuments)
              .label("noticeDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = noticeDetailsService.validateNoticeDetails(caseData);

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
