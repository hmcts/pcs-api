package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.tab.TabShowCondition;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class CaseType implements CCDConfig<PcsCase, State, UserRole> {

    public static final String CASE_TYPE_ID = "PCS";
    public static final String JURISDICTION_ID = "CIVIL";

    private static final String CASE_TYPE_NAME = "Civil Possessions";
    private static final String CASE_TYPE_DESCRIPTION = "Civil Possessions Case Type";
    private static final String JURISDICTION_NAME = "Civil Possessions";
    private static final String JURISDICTION_DESCRIPTION = "Civil Possessions Jurisdiction";

    public static String getCaseType() {
        return withChangeId(CASE_TYPE_ID, "-");
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
    public void configure(final ConfigBuilder<PcsCase, State, UserRole> builder) {
        builder.setCallbackHost(getenv().getOrDefault("CASE_API_URL", "http://localhost:3206"));

        builder.decentralisedCaseType(getCaseType(), getCaseTypeName(), CASE_TYPE_DESCRIPTION);
        builder.jurisdiction(JURISDICTION_ID, JURISDICTION_NAME, JURISDICTION_DESCRIPTION);

        var label = "Case Reference";
        builder.searchInputFields()
            .caseReferenceField()
            .field(PcsCase::getClaimPostcode, "Property postcode")
            .stateField();

        builder.searchResultFields()
            .caseReferenceField()
            .field(PcsCase::getClaimPostcode, "Property postcode")
            .stateField();

        builder.workBasketInputFields()
            .field(PcsCase::getClaimPostcode, "Property postcode");

        builder.workBasketResultFields()
            .caseReferenceField()
            .field(PcsCase::getClaimPostcode, "Property postcode")
            .stateField();

        builder.tab("summary", "Summary")
            .label("possessionDetailsLabel", null, "### Possession details")
            .field(PcsCase::getClaimAddress);

        builder.tab("CaseHistory", "History")
            .showCondition(TabShowCondition.notShowForState(State.Draft))
            .field("caseHistory");

        // ExUI won't populate model fields with their values if they are not referenced as a display field somewhere.
        // This affects the use of model fields in the State label and event show conditions for example. As a
        // workaround, the necessary fields used in the case view can be added here.
        builder.tab("hidden", "HiddenFields")
            .showCondition(NEVER_SHOW)
            .field(PcsCase::getCaseDescription) // Used in the State label
            .field(PcsCase::getPageHeadingMarkdown)
            .field(PcsCase::getUserDetails);

    }
}
