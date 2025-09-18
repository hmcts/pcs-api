package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.service.NoticeDetailsService;

import java.util.List;

/**
 * CCD page configuration for Notice Details.
 * Allows users to specify how they served notice to the defendant.
 */
@Component
@RequiredArgsConstructor
public class NoticeDetails implements CcdPageConfiguration {

    private final NoticeDetailsService noticeDetailsService;

    private static final String NOTICE_SERVICE_METHOD_CONDITION = "noticeServiceMethod=\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("noticeDetails", this::midEvent)
            .pageLabel("Notice details")
            .showCondition("noticeServed=\"Yes\"")
            .label("noticeDetails-separator", "---")
            .mandatory(PCSCase::getNoticeServiceMethod)

            // First class post
            .label("noticeDetails-firstClassPost-section", """
                <h3 class="govuk-heading-s">By first class post or other service which provides for
                delivery on the next business day</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.FIRST_CLASS_POST + "\"")
            .optional(
                PCSCase::getNoticePostedDate,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.FIRST_CLASS_POST + "\""
            )

            // Delivered to permitted place
            .label("noticeDetails-deliveredPermittedPlace-section", """
                <h3 class="govuk-heading-s">By delivering it to or leaving it at a permitted place</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.DELIVERED_PERMITTED_PLACE + "\"")
            .optional(
                PCSCase::getNoticeDeliveredDate,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.DELIVERED_PERMITTED_PLACE + "\""
            )

            // Personally handed
            .label("noticeDetails-personallyHanded-section", """
                <h3 class="govuk-heading-s">By personally handing it to or leaving it with someone</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.PERSONALLY_HANDED + "\"")
            .optional(
                PCSCase::getNoticePersonName,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.PERSONALLY_HANDED + "\""
            )
            .optional(
                PCSCase::getNoticeHandedOverDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.PERSONALLY_HANDED + "\""
            )

            // Email
            .label("noticeDetails-email-section", """
                <h3 class="govuk-heading-s">By email</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.EMAIL + "\"")
            .optional(
                PCSCase::getNoticeEmailExplanation,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.EMAIL + "\""
            )
            .optional(
                PCSCase::getNoticeEmailSentDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.EMAIL + "\""
            )

            // Other electronic method
            .label("noticeDetails-otherElectronic-section", """
                <h3 class="govuk-heading-s">By other electronic method</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER_ELECTRONIC + "\"")
            .optional(
                PCSCase::getNoticeOtherElectronicDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER_ELECTRONIC + "\""
            )

            // Other
            .label("noticeDetails-other-section", """
                <h3 class="govuk-heading-s">Other</h3>
                """, NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER + "\"")
            .optional(
                PCSCase::getNoticeOtherExplanation,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER + "\""
            )
            .optional(
                PCSCase::getNoticeOtherDateTime,
                NOTICE_SERVICE_METHOD_CONDITION + NoticeServiceMethod.OTHER + "\""
            )

            // Document upload section
            .label("noticeDetails-documentUpload-section", """
                ---
                <h2 class="govuk-heading-m">Do you want to upload a copy of the notice you served or the
                certificate of service? (Optional)</h2>
                <p class="govuk-hint">You can either upload this now or closer to the hearing date.
                Any documents you upload now will be included in the pack of documents a judge will
                receive before the hearing (the bundle).</p>

                <div class="govuk-inset-text">
                    <h3 class="govuk-heading-s">Add document</h3>
                    <p class="govuk-body">Upload a document to the system</p>
                    <a href="javascript:void(0)" class="govuk-button">Add new</a>
                </div>
                """)
              .optional(PCSCase::getNoticeDocuments);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = noticeDetailsService.validateNoticeDetails(caseData);

        if (!validationErrors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errors(validationErrors)
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
