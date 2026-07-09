package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum HearingType implements HasLabel {
    POSSESSION("Possession first hearing"),
    APPLICATION("Application"),
    ADJOURNED("Adjourned first hearing"),
    OTHER("Other");

    private final String label;
}
