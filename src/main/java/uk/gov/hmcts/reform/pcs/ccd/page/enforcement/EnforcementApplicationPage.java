package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EnforcementMultiLabel.WRIT_OR_WARRANT_CLARIFICATION;

public class EnforcementApplicationPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("enforcementApplicationPage", this::midEvent)
                .pageLabel("Your application")
                .label("enforcementApplicationPage-content", "---")
                .complex(PCSCase::getEnforcementOrder)
                .mandatory(EnforcementOrder::getSelectEnforcementType)
                .label("enforcementApplicationPage-clarification", WRIT_OR_WARRANT_CLARIFICATION);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }

}
