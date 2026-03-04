package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT_OF_RESTITUTION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WRIT;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ShowCondition;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShowConditionsEnforcementType {

    public static final ShowCondition WARRANT_FLOW = when(EnforcementOrder::getSelectEnforcementType).is(WARRANT);
    public static final ShowCondition WRIT_FLOW = when(EnforcementOrder::getSelectEnforcementType).is(WRIT);
    public static final ShowCondition WARRANT_OF_RESTITUTION_FLOW = when(EnforcementOrder::getSelectEnforcementType)
        .is(WARRANT_OF_RESTITUTION);

}
