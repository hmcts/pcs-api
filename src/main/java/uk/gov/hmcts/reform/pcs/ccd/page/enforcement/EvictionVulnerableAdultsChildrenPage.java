package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;


@AllArgsConstructor
@Component
public class EvictionVulnerableAdultsChildrenPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evictionVulnerableAdultsChildren")
            .pageLabel("Vulnerable adults and children at the property (placeholder)")
            .showCondition("anyRiskToBailiff=\"NO\" OR anyRiskToBailiff=\"NOT_SURE\"")
            .label("evictionVulnerableAdultsChildren-line-separator", "---");
    }
}
