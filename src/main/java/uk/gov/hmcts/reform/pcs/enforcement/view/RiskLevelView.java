package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevelDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskLevelService;

/**
 * Shows the current enforcement's risk level, and the reason for a high level, on the Risk assessment tab.
 */
@Component
@RequiredArgsConstructor
public class RiskLevelView {

    private final RiskLevelService riskLevelService;

    /**
     * Leaves the fields empty when the current enforcement has no risk level, or the claim has no enforcement.
     * They are cleared rather than left alone, because a draft "Enforce the order" may have replaced the
     * enforcement order on the case view with an old copy.
     */
    public void setRiskLevel(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCaseEntity == null || pcsCase == null) {
            return;
        }
        EnforcementOrderEntity enforcement = riskLevelService.findCurrentEnforcement(pcsCaseEntity)
            .filter(current -> current.getRiskLevel() != null)
            .orElse(null);
        if (enforcement == null) {
            EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
            if (enforcementOrder != null && enforcementOrder.getRiskLevelDetails() != null) {
                enforcementOrder.getRiskLevelDetails().setCurrentRiskLevel(null);
                enforcementOrder.getRiskLevelDetails().setCurrentRiskLevelReason(null);
            }
            return;
        }
        RiskLevelDetails riskLevelDetails = riskLevelDetailsOf(pcsCase);
        riskLevelDetails.setCurrentRiskLevel(enforcement.getRiskLevel());
        riskLevelDetails.setCurrentRiskLevelReason(enforcement.getRiskLevelReason());
    }

    private static RiskLevelDetails riskLevelDetailsOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
        if (enforcementOrder.getRiskLevelDetails() == null) {
            enforcementOrder.setRiskLevelDetails(new RiskLevelDetails());
        }
        return enforcementOrder.getRiskLevelDetails();
    }
}
