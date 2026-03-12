package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

public class PropertyAccessDetailsWarrantRestPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("propertyAccessDetailsRest")
            .pageLabel("Access to the property (Placeholder)")
            .showCondition(WARRANT_OF_RESTITUTION_FLOW)
            .label("propertyAccessDetailsRest-line-separator", "---")
            .label("propertyAccessDetailsRest-saveAndReturn", SAVE_AND_RETURN);
    }
}