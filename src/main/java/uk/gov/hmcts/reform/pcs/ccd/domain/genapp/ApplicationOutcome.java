package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import lombok.AllArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;

import java.util.Arrays;

/**
 * What a judge's order decided about a general application, from the decision on the make order form.
 * Listing it for a hearing leaves it open.
 */
@AllArgsConstructor
public enum ApplicationOutcome {
    GRANTED("grant"),
    DISMISSED("dismiss"),
    STRUCK_OUT("strike-out"),
    LISTED("list"),
    OTHER("other");

    /** The form field the make order form records the decision in. */
    public static final String DECISION_FIELD = "application-decision";

    private final String decision;

    public static ApplicationOutcome of(Object decision) {
        return Arrays.stream(values())
            .filter(outcome -> outcome.decision.equals(decision))
            .findFirst()
            .orElseThrow(() -> ExternalRejection.because("Select what the order decides about the application"));
    }
}
