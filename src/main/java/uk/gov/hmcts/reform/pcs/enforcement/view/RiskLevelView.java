package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevelDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskLevelService;

@Component
@RequiredArgsConstructor
public class RiskLevelView {

    private final RiskLevelService riskLevelService;

    public void setRiskAssessmentFields(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
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

    private static EnforcementOrder enforcementOrderOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        return pcsCase.getEnforcementOrder();
    }

    private static RiskLevelDetails riskLevelDetailsOf(PCSCase pcsCase) {
        EnforcementOrder enforcementOrder = enforcementOrderOf(pcsCase);
        if (enforcementOrder.getRiskLevelDetails() == null) {
            enforcementOrder.setRiskLevelDetails(new RiskLevelDetails());
        }
        return enforcementOrder.getRiskLevelDetails();
    }
}
