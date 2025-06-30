package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class PCSCaseType implements CCDConfig<PCSCase, State, UserRole> {

    private static final String CASE_TYPE_ID = "PCS";
    private static final String CASE_TYPE_NAME = "Civil Possessions";
    private static final String CASE_TYPE_DESCRIPTION = "Civil Possessions Case Type";
    private static final String JURISDICTION_ID = "PCS";
    private static final String JURISDICTION_NAME = "Possessions";
    private static final String JURISDICTION_DESCRIPTION = "Possessions Jurisdiction";

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
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.setCallbackHost(getenv().getOrDefault("CASE_API_URL", "http://localhost:3206"));

        builder.decentralisedCaseType(getCaseType(), getCaseTypeName(), CASE_TYPE_DESCRIPTION);
        builder.jurisdiction(JURISDICTION_ID, JURISDICTION_NAME, JURISDICTION_DESCRIPTION);

        var forenameLabel = "Applicant's first name";
        var surnameLabel = "Applicant's last name";
        var addressLabel = "Property Address";
        var claimantLabel = "Claimant Information";
        var genAppsLabel = "General Applications";
        var caseReferenceLabel = "Case Reference";
        builder.searchInputFields()
            .field(PCSCase::getCaseId, caseReferenceLabel)
            .field(PCSCase::getApplicantForename, forenameLabel)
            .field(PCSCase::getApplicantSurname, surnameLabel);

        builder.searchCasesFields()
            .field(PCSCase::getCaseId, caseReferenceLabel)
            .field(PCSCase::getApplicantForename, forenameLabel);

        builder.searchResultFields()
            .field(PCSCase::getCaseId, caseReferenceLabel)
            .field(PCSCase::getApplicantForename, forenameLabel)
            .field(PCSCase::getApplicantSurname, surnameLabel);

        builder.workBasketInputFields()
            .field(PCSCase::getCaseId, caseReferenceLabel)
            .field(PCSCase::getApplicantForename, forenameLabel);

        builder.workBasketResultFields()
            .field(PCSCase::getCaseId, caseReferenceLabel)
            .field(PCSCase::getApplicantForename, forenameLabel)
            .field(PCSCase::getApplicantSurname, surnameLabel);


        builder.tab("claimantInformation", claimantLabel)
            .field(PCSCase::getApplicantForename)
            .field(PCSCase::getApplicantSurname);

        builder.tab("propertyAddress", addressLabel)
            .field(PCSCase::getPropertyAddress);

        builder.tab("General Applications", genAppsLabel)
            .label("generalApplicationsMarkdownLabel", null, "${generalApplicationsSummaryMarkdown}")
            .field("generalApplicationsSummaryMarkdown", "[STATE]=\"NEVER_SHOW\"");
    }


}
