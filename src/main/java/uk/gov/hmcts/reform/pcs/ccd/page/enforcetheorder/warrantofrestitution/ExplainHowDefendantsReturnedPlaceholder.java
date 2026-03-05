package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class ExplainHowDefendantsReturnedPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("explainHowDefendantsReturned")
            .pageLabel("Explain how the defendants returned to the property after the eviction (placeholder).")
            .showCondition(WARRANT_OF_RESTITUTION_FLOW)
            .label("explainHowDefendantsReturned-line-separator", "---")
            .label("explainHowDefendantsReturned-save-and-return", SAVE_AND_RETURN);
    }
}
