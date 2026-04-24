package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Enum representing the recurrence frequency of income, benefits, and expenses.
 */
public enum RecurrenceFrequency implements HasLabel {
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    private final String label;

    RecurrenceFrequency(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
