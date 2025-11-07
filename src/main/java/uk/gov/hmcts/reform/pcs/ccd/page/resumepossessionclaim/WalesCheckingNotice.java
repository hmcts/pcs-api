package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class WalesCheckingNotice implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("walesCheckingNotice")
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
                       rel="noreferrer noopener" target="_blank" class="govuk-link"> guidance on
                       possession notice periods (opens in a new tab)</a>
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
            .label("walesCheckingNotice-save-and-return", SAVE_AND_RETURN);
    }
}
