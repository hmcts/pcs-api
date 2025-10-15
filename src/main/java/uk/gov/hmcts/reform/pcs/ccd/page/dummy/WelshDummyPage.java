package uk.gov.hmcts.reform.pcs.ccd.page.dummy;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Dummy page configuration that only works for Welsh legislative country.
 * Contains 3 radio button options for testing purposes.
 */
public class WelshDummyPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("welshDummyPage")
            .pageLabel("Welsh Dummy Page")
            .label("welshDummyPage-info", """
                ---
                <section tabindex="0">
                  <h2 class="govuk-heading-l">Tenancy or Licence Type</h2>
                  <p class="govuk-body">
                    Please select the type of tenancy or licence agreement that applies to your property in Wales.
                  </p>
                </section>
                """)
            .mandatory(PCSCase::getWelshDummyOption)
            .showCondition("legislativeCountry=\"Wales\"");
    }
}
