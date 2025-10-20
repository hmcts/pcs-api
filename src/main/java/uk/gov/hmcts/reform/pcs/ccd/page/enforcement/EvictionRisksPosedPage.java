package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;

@Component
public class EvictionRisksPosedPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("evictionRisksPosedPage", this::midEvent)
            .pageLabel("The risks posed by everyone at the property")
            .showCondition("anyRiskToBailiff=\"YES\"")
            .label("evictionRisksPosedPage-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .mandatory(EnforcementOrder::getEnforcementRiskCategories);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        
        // Initialize EnforcementOrder if null
        if (data.getEnforcementOrder() == null) {
            data.setEnforcementOrder(EnforcementOrder.builder().build());
        }
        
        // Initialize risk details if null
        if (data.getEnforcementOrder().getRiskDetails() == null) {
            data.getEnforcementOrder().setRiskDetails(EnforcementRiskDetails.builder().build());
        }
        
        // Validate that at least one category is selected
        if (data.getEnforcementOrder().getEnforcementRiskCategories() == null 
            || data.getEnforcementOrder().getEnforcementRiskCategories().isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(java.util.List.of("Select at least one option"))
                .build();
        }
        
        // Clear details for deselected categories
        if (!data.getEnforcementOrder().getEnforcementRiskCategories().contains(RiskCategory.VIOLENT_OR_AGGRESSIVE)) {
            data.getEnforcementOrder().getRiskDetails().setEnforcementViolentDetails(null);
        }
        if (!data.getEnforcementOrder().getEnforcementRiskCategories().contains(RiskCategory.FIREARMS_POSSESSION)) {
            data.getEnforcementOrder().getRiskDetails().setEnforcementFirearmsDetails(null);
        }
        if (!data.getEnforcementOrder().getEnforcementRiskCategories().contains(RiskCategory.CRIMINAL_OR_ANTISOCIAL)) {
            data.getEnforcementOrder().getRiskDetails().setEnforcementCriminalDetails(null);
        }
        
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .build();
    }
}
