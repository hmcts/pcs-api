package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.NoticeDetailsService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;

/**
 * CCD page configuration for Notice Details.
 * Allows users to specify how they served notice to the defendant.
 */
@Component
@RequiredArgsConstructor
public class NoticeDetails implements CcdPageConfiguration {

    private final NoticeDetailsService noticeDetailsService;
    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("noticeDetails", this::midEvent)
            .pageLabel("Notice details")
            .showWhen(when(PCSCase::getNoticeServed).is(YesOrNo.YES)
                .or(when(PCSCase::getWalesNoticeDetails, WalesNoticeDetails::getNoticeServed).is(YesOrNo.YES)))
            .label("noticeDetails-separator", "---")
            .complex(PCSCase::getNoticeServedDetails)
            .mandatory(NoticeServedDetails::getNoticeServiceMethod)

            // First class post
            .labelWhen("noticeDetails-firstClassPost-section", """
                <h3 class="govuk-heading-s">By first class post or other service which provides for
                delivery on the next business day</h3>
                """, noticeServiceMethodIs(NoticeServiceMethod.FIRST_CLASS_POST))
            .optionalWhen(
                NoticeServedDetails::getNoticePostedDate,
                noticeServiceMethodIs(NoticeServiceMethod.FIRST_CLASS_POST)
            )

            // Delivered to permitted place
            .labelWhen("noticeDetails-deliveredPermittedPlace-section", """
                <h3 class="govuk-heading-s">By delivering it to or leaving it at a permitted place</h3>
                """, noticeServiceMethodIs(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE))
            .optionalWhen(
                NoticeServedDetails::getNoticeDeliveredDate,
                noticeServiceMethodIs(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE)
            )

            // Personally handed
            .labelWhen("noticeDetails-personallyHanded-section", """
                <h3 class="govuk-heading-s">By personally handing it to or leaving it with someone</h3>
                """, noticeServiceMethodIs(NoticeServiceMethod.PERSONALLY_HANDED))
            .optionalWhen(
                NoticeServedDetails::getNoticePersonName,
                noticeServiceMethodIs(NoticeServiceMethod.PERSONALLY_HANDED)
            )
            .optionalWhen(
                NoticeServedDetails::getNoticeHandedOverDateTime,
                noticeServiceMethodIs(NoticeServiceMethod.PERSONALLY_HANDED)
            )

            // Email
            .labelWhen("noticeDetails-email-section", """
                <h3 class="govuk-heading-s">By email</h3>
                """, noticeServiceMethodIs(NoticeServiceMethod.EMAIL))
            .optionalWhen(
                NoticeServedDetails::getNoticeEmailExplanation,
                noticeServiceMethodIs(NoticeServiceMethod.EMAIL)
            )
            .optionalWhen(
                NoticeServedDetails::getNoticeEmailSentDateTime,
                noticeServiceMethodIs(NoticeServiceMethod.EMAIL)
            )

            // Other electronic method
            .labelWhen("noticeDetails-otherElectronic-section", """
                <h3 class="govuk-heading-s">By other electronic method</h3>
                """, noticeServiceMethodIs(NoticeServiceMethod.OTHER_ELECTRONIC))
            .optionalWhen(
                NoticeServedDetails::getNoticeOtherElectronicDateTime,
                noticeServiceMethodIs(NoticeServiceMethod.OTHER_ELECTRONIC)
            )

            // Other
            .labelWhen("noticeDetails-other-section", """
                <h3 class="govuk-heading-s">Other</h3>
                """, noticeServiceMethodIs(NoticeServiceMethod.OTHER))
            .optionalWhen(
                NoticeServedDetails::getNoticeOtherExplanation,
                noticeServiceMethodIs(NoticeServiceMethod.OTHER)
            )
            .optionalWhen(
                NoticeServedDetails::getNoticeOtherDateTime,
                noticeServiceMethodIs(NoticeServiceMethod.OTHER)
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

    private static uk.gov.hmcts.ccd.sdk.api.ShowCondition noticeServiceMethodIs(NoticeServiceMethod method) {
        return when(PCSCase::getNoticeServedDetails, NoticeServedDetails::getNoticeServiceMethod).is(method);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = noticeDetailsService.validateNoticeDetails(caseData);

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
