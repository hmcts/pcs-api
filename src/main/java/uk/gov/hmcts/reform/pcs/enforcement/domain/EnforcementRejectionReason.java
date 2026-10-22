package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Getter
public enum EnforcementRejectionReason implements HasLabel {

    CHANGE_IN_PARTY_CIRCUMSTANCES("Change in party circumstances", true),
    APPLYING_OUT_OF_TIME("Applying to enforce out of time", false),
    ORDER_NOT_ENFORCEABLE("Order not enforceable", true),
    ORDER_SET_ASIDE_OR_SUSPENDED("Possession order set aside or suspended", true),
    APPLYING_BEFORE_POSSESSION_DATE("Applying to enforce before the possession date", false),
    NOT_TRANSFERRED_TO_HIGH_COURT("Case not transferred to the high court", false),
    ANOTHER_ENFORCEMENT_JOURNEY_ONGOING("Another possession enforcement journey ongoing", false),
    DEFENDANTS_IN_BREATHING_SPACE("Defendant(s) in breathing space", false),
    OTHER("Other", true);

    private final String label;

    /** Whether the caseworker is also asked to write what they mean, rather than the option standing alone. */
    private final boolean detailRequired;

    public static List<EnforcementRejectionReason> needingDetail() {
        return Arrays.stream(values()).filter(EnforcementRejectionReason::isDetailRequired).toList();
    }
}
