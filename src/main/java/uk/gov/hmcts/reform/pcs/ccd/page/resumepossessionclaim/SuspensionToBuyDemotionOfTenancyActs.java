package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class SuspensionToBuyDemotionOfTenancyActs implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("suspensionToBuyDemotionOfTenancyActs")
            .pageLabel("Housing Act")
            .showCondition("suspensionToBuyDemotionOfTenancyPages=\"Yes\"")
            .label("suspensionToBuyDemotionOfTenancyActs-info", """
                ---
                  <ul tabindex="0">
                    <li>Section 82A(2) of the Housing Act 1985 relates to secure tenancies.</li>
                    <li>Section 6A(2) of the Housing Act 1988 relates to assured tenancies.</li>
                    <li>Section 121A of the Housing Act 1985 relates to an order suspending the right to buy for
                        secure tenancies.
                    </li>
                  </ul>
                """)
            .complex(PCSCase::getSuspensionOfRightToBuyDemotionOfTenancy)
                .mandatory(SuspensionOfRightToBuyDemotionOfTenancy::getSuspensionOfRightToBuyActs)
                .mandatory(SuspensionOfRightToBuyDemotionOfTenancy::getDemotionOfTenancyActs)
            .done()
            .label("suspensionToBuyDemotionOfTenancyActs-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

}
