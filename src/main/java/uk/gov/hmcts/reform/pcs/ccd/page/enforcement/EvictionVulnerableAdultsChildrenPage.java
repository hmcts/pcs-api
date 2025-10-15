package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;


/**
 * Full implementation will be done in another ticket - responses not captured at the moment.
 */
@AllArgsConstructor
@Component
public class EvictionVulnerableAdultsChildrenPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evictionVulnerableAdultsChildrenPage")
            .pageLabel("Vulnerable adults and children at the property (placeholder)")
            .showCondition("confirmLivingAtProperty=\"NO\"")
            .label("evictionVulnerableAdultsChildrenPage-line-separator", "---");
    }
}
