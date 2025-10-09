package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * A three-option enum for Wales-specific licensing questions.
 */
public enum WalesLicensingOption implements HasLabel {

    YES("Yes"),
    NO("No"),
    NOT_APPLICABLE("Not applicable");

    private final String label;

    WalesLicensingOption(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
