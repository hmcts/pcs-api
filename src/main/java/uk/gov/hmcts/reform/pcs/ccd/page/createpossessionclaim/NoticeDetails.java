package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

/**
 * Placeholder page configuration for the Notice Details section. Full
 * implementation will be done in another ticket - responses not captured at the
 * moment.
 */
public class NoticeDetails implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("noticeDetails")
                .pageLabel("Notice details (placeholder)")
                .showCondition("noticeServed=\"Yes\"")
                .label("noticeDetails-info",
                        """
                  ---
                    Under Development
                  """);
    }
}
