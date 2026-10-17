package uk.gov.hmcts.reform.pcs.enforcement.service;

import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

/**
 * What the caseworker decided about which kind of application, and the warrant or writ number if it was approved.
 */
public record EnforcementDecisionOutcome(SelectEnforcementType enforcementType, EnforcementDecision decision,
                                         String enforcementNumber) {

    public boolean isWrit() {
        return enforcementType == SelectEnforcementType.WRIT
            || enforcementType == SelectEnforcementType.WRIT_OF_RESTITUTION;
    }
}
