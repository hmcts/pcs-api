package uk.gov.hmcts.reform.pcs.ccd.page.updatecounterclaim;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

@Slf4j
public class SettleClaim implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("settleClaim")
            .showCondition("selectedAction=\"Settle counterclaim\"")
            .pageLabel("Settle this counterclaim")
            .label("settleClaim-info", """
                           ---
                           You are agreeing to settle this counterclaim
                           """);

    }

}
