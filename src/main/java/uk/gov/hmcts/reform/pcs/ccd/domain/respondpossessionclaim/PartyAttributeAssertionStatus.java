package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Status of a party attribute assertion.
 */
@AllArgsConstructor
@Getter
public enum PartyAttributeAssertionStatus implements HasLabel {
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under review"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected");

    private final String label;
}
