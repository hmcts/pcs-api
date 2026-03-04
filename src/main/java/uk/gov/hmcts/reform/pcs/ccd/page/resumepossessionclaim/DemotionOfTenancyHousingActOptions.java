package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;

public class DemotionOfTenancyHousingActOptions implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("demotionOfTenancyHousingActOptions")
            .pageLabel("Housing Act")
            .showCondition(
                when(PCSCase::getDemotionOfTenancy, DemotionOfTenancy::getShowHousingActsPage).is(YesOrNo.YES))
            .label("demotionOfTenancyHousingActOptions-info", """
                ---
                  <ul tabindex="0">
                    <li>Section 82A(2) of the Housing Act 1985 relates to secure tenancies.</li>
                    <li>Section 6A(2) of the Housing Act 1988 relates to assured tenancies.</li>
                  </ul>
                """)
                .complex(PCSCase::getDemotionOfTenancy)
                .mandatory(DemotionOfTenancy::getHousingAct)
                .done()
            .label("demotionOfTenancyHousingActOptions-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
