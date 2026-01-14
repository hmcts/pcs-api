package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class AmountDefendantOwesPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("AmountDefendantOwes")
            .pageLabel("The amount the defendants owe you (placeholder)")
            .showCondition("selectEnforcementType=\"WRIT\"")
            .label("amountDefendantOwes-line-separator", "---")
            .label("amountDefendantOwes-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
        ;

    }
}
