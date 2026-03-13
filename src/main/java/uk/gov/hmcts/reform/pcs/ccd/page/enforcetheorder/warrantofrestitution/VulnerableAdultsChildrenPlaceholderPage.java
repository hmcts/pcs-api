package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

@Component
public class VulnerableAdultsChildrenPlaceholderPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("warrantOfRestitutionVulnerableAdultsChildren")
            .pageLabel("Vulnerable adults and children at the property")
            .showCondition(WARRANT_OF_RESTITUTION_FLOW)
            .label("warrantOfRestitutionVulnerableAdultsChildren-line-separator", "---")
            .label("warrantOfRestitutionVulnerableAdultsChildren-saveAndReturn", SAVE_AND_RETURN);
    }
}

