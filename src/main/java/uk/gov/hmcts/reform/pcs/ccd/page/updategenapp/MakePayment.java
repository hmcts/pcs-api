package uk.gov.hmcts.reform.pcs.ccd.page.updategenapp;

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
            .page("makePaymentGA", this::midEvent)
            .showCondition("selectedAction=\"Make payment\"")
            .pageLabel("Pay the general application fee")
            .label("makePaymentGA-claimDescription", """
                           ---
                           You will be charged £10 to submit this general application.
                           """);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        log.info("Handling midEvent for MakePayment page");
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .build();
    }

}
