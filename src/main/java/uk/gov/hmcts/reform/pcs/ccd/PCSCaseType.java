package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class PCSCaseType implements CCDConfig<PCSCase, State, UserRole> {

    private static final String CASE_TYPE_ID = "PCS";
    private static final String CASE_TYPE_NAME = "Civil Possessions";
    private static final String CASE_TYPE_DESCRIPTION = "Civil Possessions";
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
        var caseReferenceLabel = "Case Reference";
        builder.searchInputFields()
            .field(PCSCase::getCaseReference, caseReferenceLabel)
            .field(PCSCase::getApplicantForename, forenameLabel)
            .field(PCSCase::getApplicantSurname, surnameLabel);

        builder.searchCasesFields()
            .field(PCSCase::getCaseReference, caseReferenceLabel)
            .field(PCSCase::getApplicantForename, forenameLabel);

        builder.searchResultFields()
            .field(PCSCase::getCaseReference, caseReferenceLabel)
            .field(PCSCase::getApplicantForename, forenameLabel)
            .field(PCSCase::getApplicantSurname, surnameLabel);

        builder.workBasketInputFields()
            .field(PCSCase::getCaseReference, caseReferenceLabel)
            .field(PCSCase::getApplicantForename, forenameLabel);

        builder.workBasketResultFields()
            .field(PCSCase::getCaseReference, caseReferenceLabel)
            .field(PCSCase::getApplicantForename, forenameLabel)
            .field(PCSCase::getApplicantSurname, surnameLabel);

        builder.tab("CaseHistory", "History")
            .field("caseHistory");

        builder.tab("claimantInformation", "Claimant Information")
            .field(PCSCase::getApplicantForename)
            .field(PCSCase::getApplicantSurname);

        builder.tab("General Applications", "General Applications")
            .label("generalApplicationsMarkdownLabel", null, "${generalApplicationsSummaryMarkdown}")
            .field("generalApplicationsSummaryMarkdown", NEVER_SHOW);

    }


}
