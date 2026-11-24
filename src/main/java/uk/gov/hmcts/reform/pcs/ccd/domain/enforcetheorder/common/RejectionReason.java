package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
@RequiredArgsConstructor
public enum RejectionReason implements HasLabel {

    CHANGE_IN_PARTY_CIRCUMSTANCES("Change in party circumstances"),
    APPLYING_OUT_OF_TIME("Applying to enforce out of time"),
    ORDER_NOT_ENFORCEABLE("Order not enforceable"),
    ORDER_SET_ASIDE_OR_SUSPENDED("Possession order set aside or suspended"),
    APPLYING_BEFORE_POSSESSION_DATE("Applying to enforce before the possession date"),
    CASE_NOT_TRANSFERRED_TO_HIGH_COURT("Case not transferred to high court"),
    ANOTHER_ENFORCEMENT_ONGOING("Another possession enforcement journey ongoing"),
    DEFENDANTS_IN_BREATHING_SPACE("Defendant(s) in breathing space"),
    OTHER("Other");

    private final String label;

}
