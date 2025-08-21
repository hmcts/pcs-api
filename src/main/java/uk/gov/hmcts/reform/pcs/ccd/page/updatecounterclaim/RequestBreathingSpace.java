package uk.gov.hmcts.reform.pcs.ccd.page.updatecounterclaim;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

@Slf4j
public class RequestBreathingSpace implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("requestBreathingSpace")
            .showCondition("selectedAction=\"Request breathing space\"")
            .pageLabel("Request breathing space")
            .label("requestBreathingSpace-info", """
                           ---
                           You are requesting 30 days of breathing space. (10 seconds for this demo version)
                           """);

    }

}
