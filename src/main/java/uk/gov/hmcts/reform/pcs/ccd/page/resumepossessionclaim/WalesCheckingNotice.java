package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.wales.WalesRentSectionRoutingService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.WALES;

@Component
@RequiredArgsConstructor
public class WalesCheckingNotice implements CcdPageConfiguration {

    private final WalesRentSectionRoutingService walesRentSectionRoutingService;
    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("walesCheckingNotice", this::midEvent)
            .pageLabel("Notice")
            .showCondition(WALES)
            .label("walesCheckingNotice-info",
                   """
                   ---
                   <section tabindex="0">
                   <p class="govuk-body">
                       If this is a possession claim under an occupation contract, you must have
                       <br> already served the defendant with notice seeking possession.
                   </p>

                   <div class="govuk-warning-text">
                     <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                     <strong class="govuk-warning-text__text">
                       <span class="govuk-visually-hidden">Warning</span>
                             Each ground requires a different notice period before a possession
                             <br> claim may be made. Some do not require any notice period.
                     </strong>
                   </div>
                   </section>
                   """)
            .complex(PCSCase::getWalesNoticeDetails)
            .mandatory(WalesNoticeDetails::getNoticeServed)
            .mandatory(WalesNoticeDetails::getTypeOfNoticeServed,"walesNoticeServed=\"Yes\"")
            .label("walesCheckingNotice-info",
                   """
                   <section tabindex="0">
                   <p class="govuk-body">
                       You must make a statement that includes:
                       <ul class="govuk-list govuk-list--bullet">
                       <li class="govuk-!-font-size-19"> the type of tenancy or licence that is in place for this
                       claim </li>
                       <li class="govuk-!-font-size-19"> how you have complied with the applicable possession
                       procedures </li>
                       </ul>
                   </p>
                   <p class="govuk-body">
                       Start your statement with:
                       <br>"I am a landlord under a tenancy or license which is not an occupation
                       <br>contract because:"
                   </p>
                   </section>
                   """,
                   "walesNoticeServed=\"No\"")
            .mandatory(WalesNoticeDetails::getNoticeStatement,"walesNoticeServed=\"No\"")
            .done()
            .label("walesCheckingNotice-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        caseData.setShowRentSectionPage(walesRentSectionRoutingService.shouldShowRentSection(caseData));

        if (caseData.getWalesNoticeDetails() != null
            && YesOrNo.NO == caseData.getWalesNoticeDetails().getNoticeServed()) {
            List<String> validationErrors = textAreaValidationService.validateSingleTextArea(
                caseData.getWalesNoticeDetails().getNoticeStatement(),
                WalesNoticeDetails.NOTICE_STATEMENT_LABEL,
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            );

            if (!validationErrors.isEmpty()) {
                return textAreaValidationService.createValidationResponse(caseData, validationErrors);
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }
}
