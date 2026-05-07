package uk.gov.hmcts.reform.pcs.ccd.page.addcasenote;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

@Component
public class AddCaseNotePage implements CcdPageConfiguration, CcdPage {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("Add a case note")
            .label(pageKey + "-line-separator", "---")
            .mandatory(PCSCase::getNote);
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
