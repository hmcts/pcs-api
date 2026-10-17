package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevelDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffEnforcementRules;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskLevelService;

import java.util.Optional;

/**
 * Shows the Risk assessment tab, with the current enforcement's risk level and the reason for a high level.
 */
@Component
@RequiredArgsConstructor
public class RiskLevelView {

    private final RiskLevelService riskLevelService;

    /**
     * Shows the tab for an approved, live warrant, the same enforcements the bailiff events work on, and
     * leaves the risk level fields empty when the current enforcement has no risk level, or the claim has no
     * enforcement. Every field is set either way rather than left alone, because a draft "Enforce the order"
     * may have replaced the enforcement order on the case view with an old copy.
     */
    public void setRiskAssessmentFields(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCaseEntity == null || pcsCase == null) {
            return;
        }
        Optional<EnforcementOrderEntity> currentEnforcement =
            riskLevelService.findCurrentEnforcement(pcsCaseEntity);
        setShowRiskAssessmentTab(pcsCase, currentEnforcement);

        EnforcementOrderEntity enforcement = currentEnforcement
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

    /**
     * A claim with no enforcement at all has nothing to clear, so it is left without an enforcement order
     * rather than given one that only says "no".
     */
    private static void setShowRiskAssessmentTab(PCSCase pcsCase,
                                                 Optional<EnforcementOrderEntity> currentEnforcement) {
        boolean show = currentEnforcement.filter(BailiffEnforcementRules::isApprovedLiveWarrant).isPresent();
        if (show || pcsCase.getEnforcementOrder() != null) {
            enforcementOrderOf(pcsCase).setShowRiskAssessmentTab(YesOrNo.from(show));
        }
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
