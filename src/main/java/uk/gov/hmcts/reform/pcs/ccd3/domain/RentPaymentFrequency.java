package uk.gov.hmcts.reform.pcs.ccd3.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Enum representing the frequency of rent payments.
 */
public enum RentPaymentFrequency implements HasLabel {
    WEEKLY("Weekly"),
    FORTNIGHTLY("Fortnightly"),
    MONTHLY("Monthly"),
    OTHER("Other");

    private final String label;

    RentPaymentFrequency(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
