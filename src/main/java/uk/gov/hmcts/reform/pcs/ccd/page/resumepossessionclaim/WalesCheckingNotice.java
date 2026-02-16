package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.wales.WalesRentSectionRoutingService;

@Component
@RequiredArgsConstructor
public class WalesCheckingNotice implements CcdPageConfiguration {

    private final WalesRentSectionRoutingService walesRentSectionRoutingService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("walesCheckingNotice", this::midEvent)
            .pageLabel("Notice of your intention to begin possession proceedings")
            .showCondition("legislativeCountry=\"Wales\"")
            .label("walesCheckingNotice-info",
                   """
                   ---
                   <section tabindex="0">
                   <p class="govuk-body">
                       You may have already served the defendants with notice of your intention to begin
                       possession proceedings. Notice periods vary between grounds and some do not require any
                       notice to be served. You should read the <a href="https://www.gov.wales/understanding-possession-process-guidance-private-landlords"
                        rel="noreferrer noopener" target="_blank" class="govuk-link">guidance on
                       possession notice periods (opens in new tab)</a>
                       to make sure your claim is valid.
                   </p>

                   <div class="govuk-warning-text">
                     <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                     <strong class="govuk-warning-text__text">
                       <span class="govuk-visually-hidden">Warning</span>
                             A judge might not grant a possession order if you have not
                             followed the correct notice procedure
                     </strong>
                   </div>
                   </section>
                   """)
            .complex(PCSCase::getWalesNoticeDetails)
            .mandatory(WalesNoticeDetails::getNoticeServed)
            .mandatory(WalesNoticeDetails::getTypeOfNoticeServed,"walesNoticeServed=\"Yes\"")
            .done()
            .label("walesCheckingNotice-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        caseData.setShowRentSectionPage(walesRentSectionRoutingService.shouldShowRentSection(caseData));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }
}
