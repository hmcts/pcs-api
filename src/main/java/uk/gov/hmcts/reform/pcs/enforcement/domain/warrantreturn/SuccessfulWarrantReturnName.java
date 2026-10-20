package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

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
