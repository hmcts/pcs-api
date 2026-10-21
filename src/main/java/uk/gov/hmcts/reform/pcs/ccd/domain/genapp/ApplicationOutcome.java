package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;

import java.util.Arrays;

/**
 * What a judge's order decided about a general application, from the decision on the make order form.
 * Listing it for a hearing leaves it open. Suspending the warrant of possession, or dismissing an application to
 * suspend it, grants or dismisses the application as any decision can.
 */
public enum ApplicationOutcome {
    GRANTED("grant", "suspend-warrant"),
    DISMISSED("dismiss", "refuse-warrant-suspension"),
    STRUCK_OUT("strike-out"),
    LISTED("list"),
    OTHER("other");

    /** The form field the make order form records the decision in. */
    public static final String DECISION_FIELD = "application-decision";

    private final String[] decisions;

    ApplicationOutcome(String... decisions) {
        this.decisions = decisions;
    }

    public static ApplicationOutcome of(Object decision) {
        return Arrays.stream(values())
            .filter(outcome -> Arrays.asList(outcome.decisions).contains(decision))
            .findFirst()
            .orElseThrow(() -> ExternalRejection.because("Select what the order decides about the application"));
    }
}
