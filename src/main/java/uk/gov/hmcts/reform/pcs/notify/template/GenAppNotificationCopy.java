package uk.gov.hmcts.reform.pcs.notify.template;

import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

public final class GenAppNotificationCopy {

    private GenAppNotificationCopy() {
    }

    public static String applicationTypePhrase(GenAppType type) {
        return switch (type) {
            case ADJOURN -> " to adjourn (delay) the hearing";
            case SET_ASIDE -> " to cancel (set aside) the order";
            default -> "";
        };
    }

    public static String applicantSubject(GenAppType type) {
        return switch (type) {
            case ADJOURN -> "Request to adjourn (delay) the hearing received";
            case SET_ASIDE -> "Request to cancel (set aside) the order received";
            default -> "Application received";
        };
    }

    public static String respondentSubject(GenAppType type) {
        return switch (type) {
            case ADJOURN -> "A party has made an application to adjourn (delay) the hearing";
            case SET_ASIDE -> "A party has made an application to cancel (set aside) the order";
            default -> "A party has made an application";
        };
    }
}
