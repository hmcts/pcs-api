package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;

public class DemotionOfTenancyHousingActOptions implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("demotionOfTenancyHousingActOptions")
            .pageLabel("Housing Act")
            .showCondition("showDemotionOfTenancyHousingActsPage=\"Yes\"")
            .label("demotionOfTenancyHousingActOptions-info", """
                ---
                  <ul>
                    <li>Section 82A(2) of the Housing Act 1985 relates to secure tenancies.</li>
                    <li>Section 6A(2) of the Housing Act 1988 relates to assured tenancies.</li>
                  </ul>
                """)
                .complex(PCSCase::getDemotionOfTenancy)
                .mandatory(DemotionOfTenancy::getDemotionOfTenancyHousingActs)
                .done();
    }
}
