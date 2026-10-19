package uk.gov.hmcts.reform.pcs.ccd.domain.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Who a message is for: a role, not a person, whose pool gets a task to review it. Judges' roles go to the judges'
 * pool; the court's other roles go to court staff's.
 */
@Getter
@AllArgsConstructor
public enum MessageRecipient implements HasLabel {
    COURT_STAFF("Court staff", false),
    CTSC_STAFF("CTSC staff", false),
    DISTRICT_JUDGE("District Judge", true),
    CIRCUIT_JUDGE("Circuit Judge", true),
    DEPUTY_DISTRICT_JUDGE("Deputy District Judge", true),
    BAILIFF("Bailiff", false),
    WELSH_LANGUAGE_UNIT("Welsh Language Unit", false);

    private final String label;
    private final boolean judicial;

    /** The role a user writes as: a judge as a District Judge, anyone else as court staff. */
    public static MessageRecipient of(boolean judge) {
        return judge ? DISTRICT_JUDGE : COURT_STAFF;
    }
}
