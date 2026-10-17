package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * The "Return name" options for a final return with the outcome "Eviction unsuccessful" or "Eviction
 * cancelled" (HDPI-7347).
 * Each option is a {@link WarrantReturnName} with the same constant name, so the list's codes and labels
 * cannot drift from it.
 */
@AllArgsConstructor
@Getter
public enum UnsuccessfulWarrantReturnName implements HasLabel {

    WARRANT_WITHDRAWN(WarrantReturnName.WARRANT_WITHDRAWN),
    WARRANT_SUSPENDED_BY_ORDER(WarrantReturnName.WARRANT_SUSPENDED_BY_ORDER),
    POSSESSION_ORDER_SET_ASIDE(WarrantReturnName.POSSESSION_ORDER_SET_ASIDE),
    UNABLE_TO_GAIN_ACCESS(WarrantReturnName.UNABLE_TO_GAIN_ACCESS),
    CLAIMANT_DID_NOT_CONFIRM_EVICTION(WarrantReturnName.CLAIMANT_DID_NOT_CONFIRM_EVICTION),
    NO_LOCKSMITH_CLAIMANT_FAILED_TO_ARRANGE(WarrantReturnName.NO_LOCKSMITH_CLAIMANT_FAILED_TO_ARRANGE),
    NO_ANIMAL_HANDLER_CLAIMANT_FAILED_TO_ARRANGE(WarrantReturnName.NO_ANIMAL_HANDLER_CLAIMANT_FAILED_TO_ARRANGE),
    NO_CLAIMANT_OR_AGENT_IN_ATTENDANCE(WarrantReturnName.NO_CLAIMANT_OR_AGENT_IN_ATTENDANCE),
    NO_REPLY_TO_REQUEST_FOR_INFORMATION(WarrantReturnName.NO_REPLY_TO_REQUEST_FOR_INFORMATION),
    EVICTION_UNSUCCESSFUL_OTHER(WarrantReturnName.EVICTION_UNSUCCESSFUL_OTHER),
    WARRANT_EXPIRED(WarrantReturnName.WARRANT_EXPIRED),
    WARRANT_ISSUED_IN_ERROR(WarrantReturnName.WARRANT_ISSUED_IN_ERROR);

    private final WarrantReturnName returnName;

    @Override
    public String getLabel() {
        return returnName.getLabel();
    }
}
