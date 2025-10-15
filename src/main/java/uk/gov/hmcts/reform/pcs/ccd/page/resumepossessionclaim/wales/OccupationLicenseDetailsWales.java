package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

/**
 * Dummy license details page configuration.
 * Contains fields for license-related information.
 */
@Slf4j
public class OccupationLicenseDetailsWales implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Place holder page ", this::midEvent)
            .pageLabel("Dummy License Details")
            .showCondition("legislativeCountry=\"Wales\"")
            .label("dummyLicenseDetails-info", """
                ---
                <section tabindex="0">
                  <h2 class="govuk-heading-l">License Details</h2>
                  <p class="govuk-body">
                    Please provide the license details for this case.
                  </p>
                </section>
                """)
            .mandatory(PCSCase::getOccupationContractLicenseDetailsOptionsWales);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                   CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
