package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class CaseType implements CCDConfig<PcsCase, State, UserRole> {

    @Override
    public void configure(final ConfigBuilder<PcsCase, State, UserRole> builder) {
        builder.setCallbackHost("http://localhost:3206");

        builder.decentralisedCaseType("PCS", "Civil Possessions", "Possessions");
        builder.jurisdiction("CIVIL", "Civil Possessions", "Civil Possessions");

        var label = "Case Description";
        builder.searchInputFields()
            .field(PcsCase::getCaseDescription, "Case Description");

        builder.searchCasesFields()
            .field(PcsCase::getCaseDescription, label);

        builder.searchResultFields()
            .field(PcsCase::getCaseDescription, label);

        builder.workBasketInputFields()
            .field(PcsCase::getCaseDescription, label);

        builder.workBasketResultFields()
            .field(PcsCase::getHyphenatedCaseRef, "Case Reference")
            .field(PcsCase::getCaseDescription, label);

        builder.tab("CaseHistory", "Case History")
            .field("caseHistory");

        builder.tab("overview", "Case Details")
            .field(PcsCase::getPropertyAddress);

        builder.tab("parties", "Case Parties")
            .field(PcsCase::getActiveParties, "", "#TABLE(forename,surname)")
            .field(PcsCase::getInactiveParties, "", "#TABLE(forename,surname)");

        builder.tab("partiesMarkdown", "Case Parties (Markdown)")
            .label("partyRolesMarkdownLabel", null, "${partyRolesMarkdown}")
            .field("partyRolesMarkdown", NEVER_SHOW);

        builder.tab("claims", "Case Claims (Markdown)")
            .label("claimsSummaryMarkdownLabel", null, "${claimsSummaryMarkdown}")
            .field("claimsSummaryMarkdown", NEVER_SHOW);

        // ExUI won't populate model fields with their values if they are not referenced as a display field somewhere.
        // This affects the use of model fields in the State label and event show conditions for example. As a
        // workaround, the necessary fields used in the case view can be added here.
        builder.tab("hidden", "HiddenFields")
            .showCondition(NEVER_SHOW)
            .field(PcsCase::getCaseDescription) // Used in the State label
            .field(PcsCase::getActivePartiesEmpty)    // Used in DeactivateParties event show condition
            .field(PcsCase::getInactivePartiesEmpty); // Used in ReactivateParties event show condition

    }

}
