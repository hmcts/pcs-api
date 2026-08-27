package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

@Getter
@AllArgsConstructor
public enum EnterGenAppType implements HasLabel {

    ADJOURN("Adjourn"),
    SET_ASIDE("Set aside"),
    SOMETHING_ELSE("Something else");

    private final String label;

    public GenAppType getStandardGenAppType() {
        return GenAppType.valueOf(name());
    }

}
