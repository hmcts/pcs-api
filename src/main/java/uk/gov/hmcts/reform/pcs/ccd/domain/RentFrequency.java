package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
public enum RentFrequency implements HasLabel {

    WEEKLY("Weekly"),
    FORTNIGHTLY("Fortnightly"),
    MONTHLY("Monthly"),
    OTHER("Other");

    private final String label;

    RentFrequency(String label) {
        this.label = label;
    }

}
