package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;



import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.BreathingSpaceNotSureContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

public class BreathingSpaceNotSurePage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("breathingSpaceNotSure", this::midEvent)
            .pageLabel(BreathingSpaceNotSureContent.HEADING)
            .showCondition(
                ShowConditionsEnforcementType.WARRANT_FLOW
                    + " AND warrantDefendantInBreathingSpace=\"NOT_SURE\""
            )
            .label("breathingSpaceNotSure-separator", "---")
            .label(
                "breathingSpaceNotSure-information",
                BreathingSpaceNotSureContent.information(false)
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
