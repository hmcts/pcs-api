package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;


@Slf4j
public class OccupationLicenceDetailsWales implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("occupationLicenceDetailsWales")
            .pageLabel("License Details (placeholder)")
            .showCondition("legislativeCountry=\"Wales\"")
            .label("occupationLicenceDetailsWales-info", """
                ---
                <section tabindex="0">
                  <h2 class="govuk-heading-l">Licene Details</h2>
                </section>
                """)
            .mandatory(PCSCase::getOccupationLicenceTypeWales);
    }
}