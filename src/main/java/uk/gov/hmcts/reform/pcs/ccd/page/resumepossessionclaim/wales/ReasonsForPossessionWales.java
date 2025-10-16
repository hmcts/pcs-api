package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

@Slf4j
public class ReasonsForPossessionWales implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("reasonsForPossessionWales")
            .pageLabel("Reasons for possession")
            .showCondition("legislativeCountry=\"Wales\"")
            .label("reasonsForPossessionWales-info", """
                ---
                <section tabindex="0">
                  <h2 class="govuk-heading-l">Reasons for possession</h2>
                  <p class="govuk-body">
                    Place holder page.
                  </p>
                </section>
                """);
    }

}
