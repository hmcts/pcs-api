package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class CaseType implements CCDConfig<PCSCase, State, UserRole> {

    private static final String CASE_TYPE_ID = "PCS";
    private static final String CASE_TYPE_NAME = "Civil Possessions";
    private static final String CASE_TYPE_DESCRIPTION = "Civil Possessions Case Type";
    private static final String JURISDICTION_ID = "PCS";
    private static final String JURISDICTION_NAME = "Possessions";
    private static final String JURISDICTION_DESCRIPTION = "Possessions Jurisdiction";

    public static String getCaseType() {
        return withChangeId(CASE_TYPE_ID, "-");
    }

    public static String getJurisdictionId() {
        return JURISDICTION_ID;
    }

    public static String getCaseTypeName() {
        return withChangeId(CASE_TYPE_NAME, " ");
    }

    private static String withChangeId(String base, String separator) {
        return ofNullable(getenv().get("CHANGE_ID"))
            .map(changeId -> base + separator + changeId)
            .orElse(base);
    }

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.setCallbackHost("http://localhost:3206");

        builder.caseType(getCaseType(), getCaseTypeName(), CASE_TYPE_DESCRIPTION);
        builder.jurisdiction(JURISDICTION_ID, JURISDICTION_NAME, JURISDICTION_DESCRIPTION);

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
            .field(PCSCase::getHyphenatedCaseRef, "Case Reference")
            .field(PCSCase::getCaseDescription, label);

        builder.tab("overview", "Case Details")
            .field(PCSCase::getPropertyAddress);

        builder.tab("parties", "Case Parties")
            .field(PCSCase::getActiveParties, "", "#TABLE(forename,surname)")
            .field(PCSCase::getInactiveParties, "", "#TABLE(forename,surname)");

        builder.tab("partiesMarkdown", "Case Parties (Markdown)")
            .label("partyRolesMarkdownLabel", null, "${partyRolesMarkdown}")
            .field("partyRolesMarkdown", NEVER_SHOW);

        builder.tab("genApps", "General Applications")
            .label("genAppsMarkdownLabel", null, "${genAppsSummaryMarkdown}")
            .field("genAppsSummaryMarkdown", NEVER_SHOW);

        builder.tab("CaseHistory", "Case History")
            .field("caseHistory");

        // ExUI won't populate model fields with their values if they are not referenced as a display field somewhere.
        // This affects the use of model fields in the State label and event show conditions for example. As a
        // workaround, the necessary fields used in the case view can be added here.
        builder.tab("hidden", "HiddenFields")
            .showCondition(NEVER_SHOW)
            .field(PCSCase::getCaseDescription) // Used in the State label
            .field(PCSCase::getActivePartiesEmpty)    // Used in DeactivateParties event show condition
            .field(PCSCase::getInactivePartiesEmpty); // Used in ReactivateParties event show condition

    }

}
