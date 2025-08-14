package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class CheckingNotice implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("checkingNotice")
                .pageLabel("Notice of your intention to begin possession proceedings")
                .label("checkingNotice-info",
                        """
                      ---
                        Under Development
                      """)
                .mandatory(PCSCase::getNoticeServed);
    }
}
