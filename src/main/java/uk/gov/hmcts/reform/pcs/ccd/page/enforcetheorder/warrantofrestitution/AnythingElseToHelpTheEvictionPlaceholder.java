package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class AnythingElseToHelpTheEvictionPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("AnythingElseThatCouldHelpWithTheEviction")
            .pageLabel("Anything else that could help with the eviction")
            .showCondition(ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW)
            .label("AnythingElseThatCouldHelpWithTheEviction-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantOfRestitutionDetails)
            .done()
            .done()
            .label("AnythingElseThatCouldHelpWithTheEviction-save-and-return", SAVE_AND_RETURN);
    }
}
