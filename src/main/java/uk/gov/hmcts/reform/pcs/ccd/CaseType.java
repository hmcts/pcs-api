package uk.gov.hmcts.reform.pcs.ccd;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;

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

    private static final String CASE_TYPE_ID = "PCS";
    public static final String CASE_TYPE_NAME = "Civil Possessions";
    public static final String CASE_TYPE_DESCRIPTION = "Civil Possessions Case Type";
    public static final String JURISDICTION_ID = "CIVIL";
    public static final String JURISDICTION_NAME = "Civil Possessions";
    public static final String JURISDICTION_DESCRIPTION = "Civil Possessions Jurisdiction";

    public static String getCaseTypeId() {
        return withChangeId(CASE_TYPE_ID, "-");
    }

    private static String withChangeId(String base, String separator) {
        return ofNullable(getenv().get("CHANGE_ID"))
            .map(changeId -> base + separator + changeId)
            .orElse(base);
    }

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.setCallbackHost(getenv().getOrDefault("CASE_API_URL", "http://localhost:3206"));

        builder.caseType(getCaseTypeId(), withChangeId(CASE_TYPE_NAME," "), CASE_TYPE_DESCRIPTION);
        builder.jurisdiction(JURISDICTION_ID, JURISDICTION_NAME, JURISDICTION_DESCRIPTION);

        var label = "Applicant Forename";
        builder.searchInputFields()
            .field(PCSCase::getApplicantForename, label);
        builder.searchCasesFields()
            .field(PCSCase::getApplicantForename, label);

        builder.searchResultFields()
            .field(PCSCase::getApplicantForename, label);
        builder.workBasketInputFields()
            .field(PCSCase::getApplicantForename, label);
        builder.workBasketResultFields()
            .field(PCSCase::getApplicantForename, label);

        builder.tab("Example", "Example Tab")
            .field(PCSCase::getApplicantForename)
            .field(PCSCase::getPartyA);
    }
}
