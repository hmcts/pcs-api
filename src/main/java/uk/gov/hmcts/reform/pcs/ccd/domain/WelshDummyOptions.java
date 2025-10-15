package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Enum representing the dummy options for Welsh legislative country only.
 * Used for radio button selection in the Welsh Dummy Page.
 */
public enum WelshDummyOptions implements HasLabel {

    SECURE("Secure"),
    STANDARD("Standard"),
    OTHER("Other");

    private final String label;

    WelshDummyOptions(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
