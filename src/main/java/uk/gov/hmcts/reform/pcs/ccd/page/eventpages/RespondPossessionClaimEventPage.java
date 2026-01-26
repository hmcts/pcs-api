package uk.gov.hmcts.reform.pcs.ccd.page.eventpages;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Component
@Slf4j
public class RespondPossessionClaimEventPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page("respondPossessionClaimEventPage", this::midEvent)
            .pageLabel("Respond To Possession Claim Event Page")
            .label("Placeholder", "This Placeholder Is Needed to trigger midevent");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        log.error("Mid event trigger");
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().build();
    }
}
