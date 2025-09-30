package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class HousingActOptions implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("housingAct")
            .pageLabel("Housing Act")
            .showCondition("alternativesToPossessionCONTAINS\"SUSPENSION_OF_RIGHT_TO_BUY\"")
            .label("housingAct-info", """
                ---
                  <ul>
                    <li>Section 82A(2) of the housing act 1985 relates to secure tenancies.</li>
                    <li>Section 6A(2) of the housing act 1988 relates to assured tenancies.</li>
                    <li>Section 121A of the housing act 1985 relates to an order suspending the right to buy for
                        secure tenancies.
                    </li>
                  </ul>
                """)
            .mandatory(PCSCase::getHousingActs);
    }
}
