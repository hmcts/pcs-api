package uk.gov.hmcts.reform.pcs.ccd.page.updatecounterclaim;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Slf4j
public class MakePayment implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("makePayment", this::midEvent)
            .showCondition("selectedAction=\"Make payment\"")
            .pageLabel("Pay the claim fee")
            .label("makePayment-claimDescription", """
                           ---
                           You will be charged £75 to submit this counterclaim.
                           """);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        log.info("Handling midEvent for MakePayment page");
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .build();
    }

}
