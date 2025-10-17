package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;


@Slf4j
public class OccupationLicenseDetailsWales implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("occupationLicenseDetailsWales")
            .pageLabel("License Details")
            .showCondition("legislativeCountry=\"Wales\"")
            .label("occupationLicenseDetailsWales-info", """
                ---
                <section tabindex="0">
                  <h2 class="govuk-heading-l">License Details</h2>
                  <p class="govuk-body">
                    Place holder page
                  </p>
                </section>
                """)
            .mandatory(PCSCase::getOccupationContractLicenceDetailsOptionsWales);
    }
}
