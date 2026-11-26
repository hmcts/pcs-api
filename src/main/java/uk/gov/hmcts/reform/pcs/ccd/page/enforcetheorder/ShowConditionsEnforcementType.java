package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShowConditionsEnforcementType {

    public static final String WARRANT_FLOW = "chooseEnforcementType=\"WARRANT\"";
    public static final String WRIT_FLOW = "chooseEnforcementType=\"WRIT\"";
    public static final String WARRANT_OF_RESTITUTION_FLOW = "chooseEnforcementType=\"WARRANT_OF_RESTITUTION\"";

    // Whether the case's latest issued order is a suspended possession order; see EnforcementOrderView.
    public static final String SUSPENDED_ORDER = "enforcementIsSuspendedOrder=\"Yes\"";
    public static final String NOT_SUSPENDED_ORDER = "enforcementIsSuspendedOrder!=\"Yes\"";

}
