package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * A three-option enum for licensing/registration questions that can be reused across different contexts.
 */
public enum LicensingOption implements HasLabel {

    YES("Yes"),
    NO("No"),
    NOT_APPLICABLE("Not applicable");

    private final String label;

    LicensingOption(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
