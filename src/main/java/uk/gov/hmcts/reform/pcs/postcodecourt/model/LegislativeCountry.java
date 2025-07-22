package uk.gov.hmcts.reform.pcs.postcodecourt.model;

import com.fasterxml.jackson.annotation.JsonValue;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;

public enum LegislativeCountry implements HasLabel {

    ENGLAND("England"),
    NORTHERN_IRELAND("Northern Ireland"),
    SCOTLAND("Scotland"),
    WALES("Wales");

    private final String label;

    LegislativeCountry(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    public static LegislativeCountry fromLabel(String label) {
        return Arrays.stream(values())
            .filter(value -> value.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No value found with label: " + label));
    }

}
