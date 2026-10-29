package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;


import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class GenAppHoldingWritPage implements CcdPageConfiguration {

    private static final String ERROR_MESSAGE =
        "You cannot continue with this application";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("genAppHoldingWritPage", this::midEvent)
            .pageLabel("General application")
            .showCondition(ShowConditionsEnforcementType.WRIT_FLOW + " AND writShowChangeNameAddressPage=\"Yes\"")
            .label("genAppHoldingWritPage-line-separator", "---")
            .label("genAppHoldingWritPage-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride(ERROR_MESSAGE)
            .build();
    }
}
