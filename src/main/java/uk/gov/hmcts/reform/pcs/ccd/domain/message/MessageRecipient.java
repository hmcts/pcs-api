package uk.gov.hmcts.reform.pcs.ccd.domain.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Who a message is for: a role, not a person, whose pool gets a task to review it. Judges' roles go to the judges'
 * pool, court staff to theirs; a role without a pool of its own is not offered until it has one.
 */
@Getter
@AllArgsConstructor
public enum MessageRecipient implements HasLabel {
    COURT_STAFF("Court staff", false),
    DISTRICT_JUDGE("District Judge", true),
    CIRCUIT_JUDGE("Circuit Judge", true),
    DEPUTY_DISTRICT_JUDGE("Deputy District Judge", true);

    private final String label;
    private final boolean judicial;

    /** The role a user writes as: a judge as a District Judge, anyone else as court staff. */
    public static MessageRecipient of(boolean judge) {
        return judge ? DISTRICT_JUDGE : COURT_STAFF;
    }
}
