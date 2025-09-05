package uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd3.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd3.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;

public class CheckingNotice implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("checkingNotice")
                .pageLabel("Notice of your intention to begin possession proceedings")
                .label("checkingNotice-info",
                        """
                        ---
                        <section tabindex="0">
                        <p class="govuk-body">
                            You may have already served the defendants with notice of your intention to begin
                            possession proceedings. Each ground has a different notice period and some
                            do not require any notice to be served. You should read the <a href="https://www.gov.uk/government/publications/understanding-the-possession-action-process-guidance-for-landlords-and-tenants/understanding-the-possession-action-process-a-guide-for-private-landlords-in-england-and-wales"
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
                .mandatory(PCSCase::getNoticeServed);
    }
}
