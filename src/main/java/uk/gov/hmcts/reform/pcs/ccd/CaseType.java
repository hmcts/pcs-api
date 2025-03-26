package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class CaseType implements CCDConfig<PCSCase, State, UserRole> {

    private static final String NEVER_SHOW = "caseDescription=\"NEVER\"";

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.setCallbackHost("http://localhost:3206");

        builder.decentralisedCaseType("PCS", "Civil Possessions", "Possessions");
        builder.jurisdiction("CIVIL", "Civil Possessions", "Civil Possessions");

        var label = "Case Description";
        builder.searchInputFields()
            .field(PCSCase::getCaseDescription, "Case Description");

        builder.searchCasesFields()
            .field(PCSCase::getCaseDescription, label);

        builder.searchResultFields()
            .field(PCSCase::getCaseDescription, label);

        builder.workBasketInputFields()
            .field(PCSCase::getCaseDescription, label);

        builder.workBasketResultFields()
            .field(PCSCase::getCaseDescription, label);

        builder.tab("overview", "Overview")
            .field(PCSCase::getPropertyAddress);

        builder.tab("parties", "All Parties")
            .field(PCSCase::getActiveParties, "", "#TABLE(forename,surname)")
            .field(PCSCase::getInactiveParties, "", "#TABLE(forename,surname)");

        builder.tab("CaseHistory", "History")
            .field("caseHistory");

        // It seems that ExUI won't populate model fields with their values if they are not referenced
        // as a display field somewhere. This affects the use of model fields in the State label and
        // event show conditions for example. As a workaround, the necessary fields can be added here.
        builder.tab("hidden", "HiddenFields")
            .showCondition(NEVER_SHOW)
            .field(PCSCase::getCaseDescription) // Used in the State label
            .field(PCSCase::getActivePartiesEmpty)    // Used in DeactivateParties event show condition
            .field(PCSCase::getInactivePartiesEmpty); // Used in ReactivateParties event show condition

    }

}
