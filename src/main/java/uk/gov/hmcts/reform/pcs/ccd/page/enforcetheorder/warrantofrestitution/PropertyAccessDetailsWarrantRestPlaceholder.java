package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

public class PropertyAccessDetailsWarrantRestPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("propertyAccessDetailsRest")
            .pageLabel("Access to the property (Placeholder)")
            .showCondition(ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW)
            .label("propertyAccessDetailsRest-line-separator", "---")
            .label("propertyAccessDetailsRest-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}