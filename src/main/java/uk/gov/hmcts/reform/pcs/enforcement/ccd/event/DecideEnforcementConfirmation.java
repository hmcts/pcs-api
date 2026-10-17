package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionOutcome;

import java.util.Objects;

/**
 * The decideEnforcement confirmation screen: "Warrant approved", "Writ rejected" and so on.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DecideEnforcementConfirmation {

    private static final String PANEL = """
        ---
        <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
        <span class="govuk-panel__title govuk-!-font-size-36">%s</span><br>
        <span class="govuk-panel__body govuk-!-font-size-24">Case number: %s</span><br>
        %s<span class="govuk-panel__body govuk-!-font-size-24">Property address: %s</span><br>
        <span class="govuk-panel__body govuk-!-font-size-24">%s</span>
        </div>

        <h3 class="govuk-heading-s">What happens next</h3>
        <p class="govuk-body govuk-!-margin-bottom-6">%s</p>
        """;

    private static final String NUMBER_LINE = """
        <span class="govuk-panel__body govuk-!-font-size-24">%s number: %s</span><br>
        """;

    static String render(EnforcementDecisionOutcome outcome, long caseReference, String propertyAddress,
                         String caseName) {
        String kind = outcome.isWrit() ? "Writ" : "Warrant";
        boolean approved = outcome.decision() == EnforcementDecision.ACCEPT;
        String title = kind + (approved ? " approved" : " rejected");
        String numberLine = approved ? NUMBER_LINE.formatted(kind, outcome.enforcementNumber()) : "";
        return PANEL.formatted(title, caseReference, numberLine, Objects.toString(propertyAddress, ""),
                               Objects.toString(caseName, ""), whatHappensNext(outcome, approved));
    }

    private static String whatHappensNext(EnforcementDecisionOutcome outcome, boolean approved) {
        if (!approved) {
            return "The claimant can apply again.";
        }
        if (outcome.isWrit()) {
            return "The claim has been transferred to the High Court. The claimant needs to give the writ"
                + " to a High Court Enforcement Officer.";
        }
        return "The warrant will be allocated to a bailiff.";
    }
}
