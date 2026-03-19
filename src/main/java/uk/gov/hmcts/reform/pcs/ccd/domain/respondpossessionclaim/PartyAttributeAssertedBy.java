package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Who asserted a correction to a party attribute.
 */
@AllArgsConstructor
@Getter
public enum PartyAttributeAssertedBy implements HasLabel {
    CLAIMANT("Claimant"),
    DEFENDANT("Defendant"),
    JUDGE("Judge"),
    COURT_STAFF("Court staff"),
    CASE_WORKER("Case worker");

    private final String label;
}
