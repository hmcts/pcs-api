package uk.gov.hmcts.reform.pcs.ccd.page.linkdefendant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class LinkCode implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("linkCode")
                .pageLabel("Provide linking code")
                .mandatory(PCSCase::getLinkCode)
                .mandatory(PCSCase::getLinkUserId);
    }
}
