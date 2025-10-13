package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum SelectEnforcementType implements HasLabel {

    WARRENT("Warrent of possession"),
    WRIT("Writ of possession");

    private String label;
}
