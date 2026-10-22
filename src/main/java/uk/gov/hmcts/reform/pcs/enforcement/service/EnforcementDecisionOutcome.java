package uk.gov.hmcts.reform.pcs.enforcement.service;

import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

public record EnforcementDecisionOutcome(SelectEnforcementType enforcementType, EnforcementDecision decision,
                                         String enforcementNumber) {

    public boolean isWrit() {
        return enforcementType == SelectEnforcementType.WRIT
            || enforcementType == SelectEnforcementType.WRIT_OF_RESTITUTION;
    }
}
