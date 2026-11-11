package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/** What an order decides about a general application, as the make order form's answer for it gives it. */
public enum GenAppDecision {

    GRANT("grant"),
    DISMISS("dismiss"),
    STRIKE_OUT("strike-out"),
    /** Listed for a hearing on notice, which leaves the application to be decided. */
    LIST("list"),
    OTHER("other");

    private final String answer;

    GenAppDecision(String answer) {
        this.answer = answer;
    }

    /** The order's decision about the application, if its answers decide it; "Not dealt with" decides nothing. */
    public static Optional<GenAppDecision> in(Map<String, Object> formData, GenAppEntity genApp) {
        Object answer = formData == null ? null : formData.get("app-" + genApp.getId() + "-decision");
        return Arrays.stream(values()).filter(decision -> decision.answer.equals(answer)).findFirst();
    }
}
