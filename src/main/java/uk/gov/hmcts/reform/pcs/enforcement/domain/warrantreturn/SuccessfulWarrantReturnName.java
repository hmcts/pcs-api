package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * The "Return name" options for a final return with the outcome "Eviction successful" (HDPI-7347).
 * Each option is a {@link WarrantReturnName} with the same constant name, so the list's codes and labels
 * cannot drift from it.
 */
@AllArgsConstructor
@Getter
public enum SuccessfulWarrantReturnName implements HasLabel {

    EVICTION_SUCCESSFUL(WarrantReturnName.EVICTION_SUCCESSFUL);

    private final WarrantReturnName returnName;

    @Override
    public String getLabel() {
        return returnName.getLabel();
    }
}
