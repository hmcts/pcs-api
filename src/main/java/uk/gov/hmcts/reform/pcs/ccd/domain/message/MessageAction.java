package uk.gov.hmcts.reform.pcs.ccd.domain.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/** Whether the user is starting a message or answering one. */
@Getter
@AllArgsConstructor
public enum MessageAction implements HasLabel {
    SEND("Send a new message"),
    REPLY("Reply to a message");

    private final String label;
}
