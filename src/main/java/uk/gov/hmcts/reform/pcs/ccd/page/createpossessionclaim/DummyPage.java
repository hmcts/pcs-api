package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

/**
 * CCD page configuration for making a housing possession claim online.
 */
@Slf4j
public class DummyPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("startTheService", this::midEvent)
            .label("dummyContent",
                   "Testing Testing 123 123"
            );
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        log.error("------- ----- Mid Event Run ------- -----");
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().build();
    }

}
