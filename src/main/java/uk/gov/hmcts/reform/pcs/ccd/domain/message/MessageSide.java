package uk.gov.hmcts.reform.pcs.ccd.domain.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** The side a user writes a message as: court staff, or a judge of whatever rank. */
@Getter
@AllArgsConstructor
public enum MessageSide {
    COURT_STAFF("Court staff"),
    JUDGE("Judge");

    private final String label;

    public static MessageSide of(boolean judge) {
        return judge ? JUDGE : COURT_STAFF;
    }

    public boolean isJudicial() {
        return this == JUDGE;
    }
}
