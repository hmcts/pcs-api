package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class EvictionVulnerableAdultsChildrenPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evictionVulnerableAdultsChildren")
            .pageLabel("Vulnerable adults and children at the property (placeholder)")
            .label("evictionVulnerableAdultsChildren-line-separator", "---");
    }
}
