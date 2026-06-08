package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.CanUploadNoticeServedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.NoticeDetailsService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NoticeDetails implements CcdPageConfiguration {

    private final NoticeDetailsService noticeDetailsService;

    private static final String NOTICE_SERVICE_METHOD_CONDITION = "notice_ServiceMethod=\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("noticeDetails", this::midEvent)
            .pageLabel("Notice details")
            .showCondition("noticeServed=\"Yes\""
                               + " OR walesNoticeServed=\"Yes\"")
            .label("noticeDetails-separator", "---")
            .complex(PCSCase::getNoticeServedDetails)
            .mandatory(NoticeServedDetails::getServiceMethod)

            // First class post
            .label("noticeDetails-firstClassPost-section", """
                <h3 class="govuk-heading-s">By first class post or other service which provides for
                delivery on the next business day</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.FIRST_CLASS_POST + "\"")
            .optional(
                NoticeServedDetails::getPostedDate,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.FIRST_CLASS_POST + "\""
            )

            // Delivered to permitted place
            .label("noticeDetails-deliveredPermittedPlace-section", """
                <h3 class="govuk-heading-s">By delivering it to or leaving it at a permitted place</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.DELIVERED_PERMITTED_PLACE + "\"")
            .optional(
                NoticeServedDetails::getDeliveredDate,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.DELIVERED_PERMITTED_PLACE + "\""
            )

            // Personally handed
            .label("noticeDetails-personallyHanded-section", """
                <h3 class="govuk-heading-s">By personally handing it to or leaving it with someone</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.PERSONALLY_HANDED + "\"")
            .optional(
                NoticeServedDetails::getPersonName,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.PERSONALLY_HANDED + "\""
            )
            .optional(
                NoticeServedDetails::getHandedOverDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.PERSONALLY_HANDED + "\""
            )

            // Email
            .label("noticeDetails-email-section", """
                <h3 class="govuk-heading-s">By email</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.EMAIL + "\"")
            .optional(
                NoticeServedDetails::getEmailAddress,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.EMAIL + "\""
            )
            .optional(
                NoticeServedDetails::getEmailSentDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.EMAIL + "\""
            )

            // Other electronic method
            .label("noticeDetails-otherElectronic-section", """
                <h3 class="govuk-heading-s">By other electronic method</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER_ELECTRONIC + "\"")
            .optional(
                NoticeServedDetails::getOtherElectronicExplanation,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER_ELECTRONIC + "\""
            )
            .optional(
                NoticeServedDetails::getOtherElectronicDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER_ELECTRONIC + "\""
            )

            // Other
            .label("noticeDetails-other-section", """
                <h3 class="govuk-heading-s">Other</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER + "\"")
            .optional(
                NoticeServedDetails::getOtherExplanation,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER + "\""
            )
            .optional(
                NoticeServedDetails::getOtherDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER + "\""
            )

            // Document upload section
            .mandatory(NoticeServedDetails::getAbleToUploadDocument)
            .mandatory(NoticeServedDetails::getDocuments,
                    "notice_AbleToUploadDocument=\"YES\"")
            .mandatory(NoticeServedDetails::getUnableToUploadReason,
                    "notice_AbleToUploadDocument=\"NO\"")
            .label("noticeDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        if (CanUploadNoticeServedDocument.YES.equals(caseData.getNoticeServedDetails().getAbleToUploadDocument())) {
            caseData.getNoticeServedDetails().setUnableToUploadReason(null);
        }

        List<String> validationErrors = noticeDetailsService.validateNoticeDetails(caseData);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errors(validationErrors)
                .build();
    }
}
