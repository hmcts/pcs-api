package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Enum representing the recurrence frequency of income, benefits, and expenses.
 */
@AllArgsConstructor
@Getter
public enum RecurrenceFrequency implements HasLabel {
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    private final String label;

}
