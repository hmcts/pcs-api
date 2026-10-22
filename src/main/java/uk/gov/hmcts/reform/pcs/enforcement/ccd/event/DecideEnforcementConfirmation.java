package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionOutcome;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DecideEnforcementConfirmation {

    private static final String BODY = """
        <p class="govuk-body">Case number: %s</p>
        %s<p class="govuk-body">Property address: %s</p>
        <p class="govuk-body">%s</p>

        <h2 class="govuk-heading-m">What happens next</h2>
        <p class="govuk-body govuk-!-margin-bottom-6">%s</p>
        """;

    private static final String NUMBER_LINE = """
        <p class="govuk-body">%s number: %s</p>
        """;

    static String header(EnforcementDecisionOutcome outcome) {
        return "# " + outcome.enforcementType().getLabel() + (approved(outcome) ? " approved" : " rejected");
    }

    static String body(EnforcementDecisionOutcome outcome, String formattedCaseReference, String propertyAddress,
                       String caseName) {
        String numberLine = approved(outcome)
            ? NUMBER_LINE.formatted(outcome.isWrit() ? "Writ" : "Warrant", outcome.enforcementNumber())
            : "";
        return BODY.formatted(formattedCaseReference, numberLine, Objects.toString(propertyAddress, ""),
                              Objects.toString(caseName, ""), whatHappensNext(outcome));
    }

    private static boolean approved(EnforcementDecisionOutcome outcome) {
        return outcome.decision() == EnforcementDecision.ACCEPT;
    }

    private static String whatHappensNext(EnforcementDecisionOutcome outcome) {
        if (!approved(outcome)) {
            return "The claimant can apply again.";
        }
        if (outcome.isWrit()) {
            return "The sealed writ will be added to the ‘Enforcement’ folder in the case file view, where the"
                + " claimant can see it. The claimant needs to give the writ to a High Court Enforcement Officer,"
                + " who will then contact them to arrange the eviction. There is no further enforcement step for"
                + " the court on this writ.";
        }
        return "The warrant will be added to the ‘Enforcement’ folder in the case file view. The case now needs"
            + " to be allocated to a bailiff.";
    }
}
