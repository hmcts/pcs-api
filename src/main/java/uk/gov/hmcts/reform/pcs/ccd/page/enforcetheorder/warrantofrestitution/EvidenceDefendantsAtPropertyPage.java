package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

public class EvidenceDefendantsAtPropertyPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evidenceDefendantsAtProperty")
            .pageLabel("Evidence that the defendants are at the property")
            .showCondition(WARRANT_OF_RESTITUTION_FLOW)
            .label("evidenceDefendantsAtProperty-line-separator", "---")
            .label("evidenceDefendantsAtProperty-save-and-return", SAVE_AND_RETURN);
    }
}

