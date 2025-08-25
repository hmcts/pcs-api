package uk.gov.hmcts.reform.pcs.ccd.page.updategenapp;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

@Slf4j
public class RejectGenApp implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("rejectGenApp")
            .showCondition("selectedAction=\"Reject general application\"")
            .pageLabel("Reject general application")
            .label("rejectGenApp-info", """
                           ---
                           You are rejecting this general application
                           """);

    }

}
