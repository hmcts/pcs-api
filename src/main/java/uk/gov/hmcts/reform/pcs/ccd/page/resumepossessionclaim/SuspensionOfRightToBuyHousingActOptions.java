package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;

public class SuspensionOfRightToBuyHousingActOptions implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("suspensionOfRightToBuyHousingActOptions")
            .pageLabel("Housing Act")
            .showCondition("showSuspensionOfRightToBuyHousingActsPage=\"Yes\"")
            .label("suspensionOfRightToBuyHousingActOptions-info", """
                ---
                  <ul>
                    <li>Section 82A(2) of the Housing Act 1985 relates to secure tenancies.</li>
                    <li>Section 6A(2) of the Housing Act 1988 relates to assured tenancies.</li>
                    <li>Section 121A of the Housing Act 1985 relates to an order suspending the right to buy for
                        secure tenancies.
                    </li>
                  </ul>
                """)
                .complex(PCSCase::getSuspensionOfRightToBuy)
                .mandatory(SuspensionOfRightToBuy::getSuspensionOfRightToBuyHousingActs)
                .done();
    }
}
