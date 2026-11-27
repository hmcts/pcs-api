package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Send Letter's status for a letter. A letter is pending until it reaches a final status; Send Letter can
 * still move a {@code FailedToUpload} letter on, so that one is pending too.
 */
public enum LetterStatus {
    CREATED("Created", false, false),
    UPLOADED("Uploaded", false, false),
    FAILED_TO_UPLOAD("FailedToUpload", false, false),
    POSTED("Posted", true, false),
    POSTED_LOCALLY("PostedLocally", true, false),
    ABORTED("Aborted", true, true),
    NOT_SENT("NotSent", true, true),
    NO_REPORT_ABORTED("NoReportAborted", true, true),
    SKIPPED("Skipped", true, true);

    private final String sendLetterName;
    private final boolean isFinal;
    private final boolean failed;

    LetterStatus(String sendLetterName, boolean isFinal, boolean failed) {
        this.sendLetterName = sendLetterName;
        this.isFinal = isFinal;
        this.failed = failed;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean isFailed() {
        return failed;
    }

    public static Optional<LetterStatus> fromSendLetter(String sendLetterName) {
        return Arrays.stream(values())
            .filter(status -> status.sendLetterName.equals(sendLetterName))
            .findFirst();
    }

    public static List<LetterStatus> pending() {
        return Arrays.stream(values()).filter(status -> !status.isFinal).toList();
    }
}
