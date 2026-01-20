package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

public class AmountDefendantOwesPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("amountDefendantOwes")
            .pageLabel("The amount the defendants owe you (placeholder)")
            .showCondition(ShowConditionsWarrantOrWrit.WRIT_FLOW)
            .label("amountDefendantOwes-line-separator", "---")
            .label("amountDefendantOwes-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
