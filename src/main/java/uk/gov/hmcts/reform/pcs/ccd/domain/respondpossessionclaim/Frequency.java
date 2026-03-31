package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Enum representing the frequency of income and expenses.
 */
@AllArgsConstructor
@Getter
public enum Frequency implements HasLabel {
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    private final String label;

}
