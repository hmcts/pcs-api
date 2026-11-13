package uk.gov.hmcts.reform.pcs.notify.template;

import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

import java.util.Map;

public final class GenAppNotificationCopy {

    public record Copy(String applicationTypePhrase, String applicantSubject, String respondentSubject) {
    }

    private static final Copy DEFAULT = new Copy(
        "",
        "Application received",
        "A party has made an application"
    );

    private static final Map<GenAppType, Copy> BY_TYPE = Map.of(
        GenAppType.ADJOURN, new Copy(
            " to adjourn (delay) the hearing",
            "Request to adjourn (delay) the hearing received",
            "A party has made an application to adjourn (delay) the hearing"
        ),
        GenAppType.SET_ASIDE, new Copy(
            " to cancel (set aside) the order",
            "Request to cancel (set aside) the order received",
            "A party has made an application to cancel (set aside) the order"
        )
    );

    private GenAppNotificationCopy() {
    }

    public static Copy forType(GenAppType type) {
        return BY_TYPE.getOrDefault(type, DEFAULT);
    }
}
