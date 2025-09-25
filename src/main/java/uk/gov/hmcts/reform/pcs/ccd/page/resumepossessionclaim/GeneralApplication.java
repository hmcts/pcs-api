package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class GeneralApplication implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("generalApplication")
                .pageLabel("Applications")
                .label("generalApplication-info",
                        """
                  ---
                  <section tabindex="0">
                    <p class="govuk-body">
                    If you need to ask the court for something before your case can be given 
                    a hearing date or sent to the defendants, you'll need to make an application.
                    </p>
                    <p class="govuk-body">
                    Applications could include:
                    </p>

                    <ul class="govuk-list govuk-list--bullet">
                        <li class="govuk-!-font-size-19">permission to make your claim</li>
                        <li class="govuk-!-font-size-19">permission to send papers to a defendant 
                          outside England or Wales 
                        </li>
                        <li class="govuk-!-font-size-19">a request for a hearing without notifying the defendants</li>
                    </ul>

                    <p class="govuk-body">
                    After you submit and pay for your claim, you'll have until the end of the 
                    next working day to make your application. If you do it any later, your application 
                    will not be considered and a hearing will be scheduled as normal.
                    </p>

                    <p class="govuk-body">
                    If you're not sure whether you need to make an application as part of your claim, 
                    you should get legal advice.
                    </p>
                  </section>

                  """)
                .mandatory(PCSCase::getGeneralApplicationWanted);
    }
}
