package uk.gov.hmcts.reform.pcs.ccd.page.updategenapp;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

@Slf4j
public class AcceptGenApp implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("acceptGenApp")
            .showCondition("selectedAction=\"Accept general application\"")
            .pageLabel("Accept general application")
            .label("acceptGenApp-info", """
                           ---
                           You are accepting this general application
                           """);

    }

}
