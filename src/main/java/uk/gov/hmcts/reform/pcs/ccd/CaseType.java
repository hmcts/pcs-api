package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.MY_APPLICANT_CASE_ROLE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.MY_RESPONDENT_ROLE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.MY_JUDGE_ROLE;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class CaseType implements CCDConfig<PcsCase, State, UserRole> {

    public static final String PCS_CASE_TYPE = "PCS";
    public static final String PCS_JURISDICTION = "CIVIL";

    @Override
    public void configure(final ConfigBuilder<PcsCase, State, UserRole> builder) {
        builder.setCallbackHost("http://localhost:3206");

        builder.decentralisedCaseType(PCS_CASE_TYPE, "Civil Possessions", "Possessions");
        builder.jurisdiction(PCS_JURISDICTION, "Civil Possessions", "The new one");

        var label = "Case Description";
        builder.searchInputFields()
            .field(PcsCase::getCaseDescription, label);
        builder.searchCasesFields()
            .field(PcsCase::getCaseDescription, label);

        builder.searchResultFields()
            .field(PcsCase::getCaseDescription, label);
        builder.workBasketInputFields()
            .field(PcsCase::getCaseDescription, label);
        builder.workBasketResultFields()
            .field(PcsCase::getHyphenatedCaseRef, "Case Reference")
            .field(PcsCase::getCaseDescription, label);

        builder.tab("userRoles", "User Roles")
            .label("roleMarkdownLabel", null, "${roleMarkdown}");

        builder.tab("caseSummaryExUi", "Case Summary (ExUI)")
            .label("caseSummaryLabel", null, "### Case Summary")
            .label("caseSummaryApplicantLabel", null, "#### Applicant")
            .field(PcsCase::getApplicantName)
            .field(PcsCase::getApplicantAddress)
            .label("caseSummaryRespondentLabel", null, "#### Respondent")
            .field(PcsCase::getRespondentName)
            .field(PcsCase::getRespondentAddress);

        builder.tab("caseSummary", "Case Summary")
            .label("caseSummaryMarkdownLabel", null, "${caseSummaryMarkdown}");

        builder.tab("events", "Update Case")
            .label("eventsMarkdownLabel", null, "${eventsMarkdown}");

        builder.tab("detailsForApplicant", "Details for Applicant")
            .forRoles(MY_APPLICANT_CASE_ROLE, MY_JUDGE_ROLE)
            .label("detailsForApplicantLabel", null, "${detailsForApplicantMarkdown}");

        builder.tab("detailsForRespondent", "Details for Respondent")
            .forRoles(MY_RESPONDENT_ROLE, MY_JUDGE_ROLE)
            .label("detailsForRespondentLabel", null, "${detailsForRespondentMarkdown}");

        builder.tab("CaseHistory", "History")
            .field("caseHistory");

        // ExUI won't populate model fields with their values if they are not referenced as a display field somewhere.
        // This affects the use of model fields in the State label and event show conditions for example. As a
        // workaround, the necessary fields used in the case view can be added here.
        builder.tab("hidden", "HiddenFields")
            .showCondition(NEVER_SHOW)
            .field(PcsCase::getRoleMarkdown)
            .field(PcsCase::getCaseSummaryMarkdown)
            .field(PcsCase::getEventsMarkdown)
            .field(PcsCase::getDetailsForApplicantMarkdown)
            .field(PcsCase::getDetailsForRespondentMarkdown)
            .field(PcsCase::getCaseDescription);

    }
}
