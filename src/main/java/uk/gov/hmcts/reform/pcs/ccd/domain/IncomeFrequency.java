package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Enum representing the frequency of income and benefit payments.
 */
public enum IncomeFrequency implements HasLabel {
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    private final String label;

    IncomeFrequency(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
