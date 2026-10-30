package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;



import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.BreathingSpaceNotSureContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

public class BreathingSpaceNotSureWritPage implements CcdPageConfiguration {
    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("breathingSpaceNotSureWrit", this::midEvent)
            .pageLabel(BreathingSpaceNotSureContent.HEADING)
            .showCondition(
                ShowConditions.and(ShowConditionsEnforcementType.WRIT_FLOW,
                                   "writDefendantInBreathingSpace=\"NOT_SURE\""))
            .label("breathingSpaceNotSureWrit-separator", "---")
            .label(
                "breathingSpaceNotSureWrit-information",
                BreathingSpaceNotSureContent.information(true)
            );
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride(
                BreathingSpaceNotSureContent.ERROR_MESSAGE
            )
            .build();
    }


}
