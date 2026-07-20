package uk.gov.hmcts.reform.pcs.ccd.page.deletecase;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

@AllArgsConstructor
@Component
public class DeleteThisCasePage implements CcdPageConfiguration, CcdPage {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();

        pageBuilder
            .page(pageKey)
            .pageLabel("Delete this case")
            .label(pageKey + "-separator", "---")
            .mandatory(PCSCase::getDeleteUnsubmittedClaim);
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
