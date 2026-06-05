package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class CaseType implements CCDConfig<PCSCase, State, AccessProfile> {

    private static final String CASE_TYPE_ID = "PCS";
    private static final String CASE_TYPE_NAME = "Possession";
    private static final String CASE_TYPE_DESCRIPTION = "Possession Case Type";
    private static final String JURISDICTION_ID = "PCS";
    private static final String JURISDICTION_NAME = "Civil Possession";
    private static final String JURISDICTION_DESCRIPTION = "Civil Possession Jurisdiction";

    @Value("${hmcts.hmctsOrgId}")
    private String hmctsServiceId;

    @Value("${caseApi.url}")
    private String caseApiUrl;

    public static String getCaseType() {
        return withSuffix(CASE_TYPE_ID, "-");
    }

    public static String getJurisdictionId() {
        return JURISDICTION_ID;
    }

    public static String getCaseTypeName() {
        return withSuffix(CASE_TYPE_NAME, " ");
    }

    private static String withSuffix(String base, String separator) {
        return ofNullable(getenv().get("CASE_TYPE_SUFFIX"))
            .map(changeId -> base + separator + changeId)
            .orElse(base);
    }

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.setCallbackHost(caseApiUrl);

        builder.caseType(getCaseType(), getCaseTypeName(), CASE_TYPE_DESCRIPTION);
        builder.jurisdiction(JURISDICTION_ID, JURISDICTION_NAME, JURISDICTION_DESCRIPTION);
        builder.hmctsServiceId(hmctsServiceId);

        builder.searchInputFields()
            .caseReferenceField();

        builder.searchCasesFields()
            .caseReferenceField();

        builder.searchResultFields()
            .caseReferenceField();

        builder.workBasketResultFields()
            .caseReferenceField()
            .field(PCSCase::getPropertyAddress, "Property Address");

        builder.tab("nextSteps", "Next steps")
            .showCondition(ShowConditions.stateEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .label("nextStepsMarkdownLabel", null, "${nextStepsMarkdown}")
            .field("nextStepsMarkdown", NEVER_SHOW);

        buildCasePartiesTab(builder);

        buildCaseDetailsTab(builder);

        builder.tab("caseFileView", "Case File View")
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field(PCSCase::getCaseFileView, null, "#ARGUMENT(CaseFileView)");

        buildSummaryTab(builder);

        builder.tab("CaseHistory", "History")
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field("caseHistory");

        builder.tab("hidden", "HiddenFields")
            .showCondition(NEVER_SHOW)
            .field(PCSCase::getCaseTitleMarkdown)
            .field(PCSCase::getDashboardData);

        builder.tab("serviceRequest", "Service Request")
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field("waysToPay");

        buildCaseNotesTab(builder);

        builder.tab("caseLinks", "Linked Cases")
            .forRoles(AccessProfile.PCS_SOLICITOR)
            .field(PCSCase::getLinkedCasesComponentLauncher, null, "#ARGUMENT(LinkedCases)")
            .field(PCSCase::getCaseLinks, "LinkedCasesComponentLauncher!=\"\"", "#ARGUMENT(LinkedCases)");

        builder.tab("caseFlags", "Case flags")
            .forRoles(AccessProfile.JUDGE, AccessProfile.FEE_PAID_JUDGE, AccessProfile.CIRCUIT_JUDGE,
                AccessProfile.LEADERSHIP_JUDGE,
                AccessProfile.CTSC_ADMIN,
                AccessProfile.HEARING_CENTRE_ADMIN,
                AccessProfile.WLU_ADMIN)
            .field(PCSCase::getFlagLauncherInternal, null, "#ARGUMENT(READ)")
            .field(PCSCase::getCaseFlags, "flagLauncherInternal!=\"\"")
            .field(PCSCase::getParties, "flagLauncherInternal!=\"\"", "#ARGUMENT(Flags)");

        configureCaseFileCategories(builder);
    }

    private void configureCaseFileCategories(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        for (CaseFileCategory category : CaseFileCategory.values()) {
            builder.categories(AccessProfile.PCS_SOLICITOR)
                .categoryID(category.getId())
                .categoryLabel(category.getLabel())
                .displayOrder(category.getDisplayOrder())
                .build();
        }
    }

    private void buildCaseNotesTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("notes", "Notes")
            .field(PCSCase::getCaseNotes);
    }

    private void buildCasePartiesTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("caseParties", "Case Parties")
            .label("Case Parties", null, "#### Case Parties")
            .field("casePartiesTab_ClaimantDetails")
            .field("casePartiesTab_DefendantOneDetails")
            .field("casePartiesTab_DefendantsDetails");
    }

    private void buildSummaryTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("summary", "Summary")
            .label("confirmEvictionSummaryMarkupLabel", null, "${confirmEvictionSummaryMarkup}")
            .field("confirmEvictionSummaryMarkup", NEVER_SHOW)
            .label("Summary", null, "## Summary")
            .field("summaryTab_RepossessedPropertyAddress")
            .field("summaryTab_GroundsForPossession")
            .field("summaryTab_ReasonsForPossession")
            .field("summaryTab_DateClaimSubmitted")
            .label("Claimant details",
                   "summaryTab_ClaimantDetails!=\"\"",
                   "## Claimant details")
            .field("summaryTab_ClaimantDetails")
            .label("Defendant details",
                   "summaryTab_DefendantDetails!=\"\"",
                   "## Defendant details")
            .field("summaryTab_DefendantDetails")
            .field("summaryTab_AdditionalDefendants")
            .label("Rent arrears",
                   "summaryTab_RentArrearsDetails!=\"\"",
                   "## Rent arrears")
            .field("summaryTab_RentArrearsDetails")
            .label("Tenancy or occupation contract or licence",
                   "summaryTab_TenancyDetails!=\"\"",
                   "## Tenancy, occupation contract or licence")
            .field("summaryTab_TenancyDetails")
            .label("Notice",
                   "summaryTab_NoticeDetails!=\"\"",
                   "## Notice")
            .field("summaryTab_NoticeDetails");
    }

    private void buildCaseDetailsTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("caseDetails", "Case Details")
            .label("Case details", null, "# Case details")
            .field("detailsTab_ClaimDetails")
            .field("detailsTab_PropertyAddress")
            .field("detailsTab_GroundsForPossessionDetails")
            .field("detailsTab_DateClaimSubmitted")
            .field("detailsTab_TenancyLicenceDetails")
            .field("detailsTab_NoticeDetails")
            .field("detailsTab_ActionsTakenDetails")
            .field("detailsTab_RentArrearsDetails")
            .field("detailsTab_ReasonsForPossessionDetails")
            .field("detailsTab_ApplicationsDetails")
            .label(
                "Claimant Details",
                "detailsTab_ClaimantInformation!=\"\"",
                "## Claimant Details"
            )
            .field("detailsTab_ClaimantInformation")
            .field("detailsTab_ClaimantAddress")
            .field("detailsTab_ClaimantContactDetails")
            .field("detailsTab_ClaimantCircumstances")
            .label(
                "Defendant Details",
                "detailsTab_DefendantInformationDetails!=\"\"",
                "## Defendant Details"
            )
            .field("detailsTab_DefendantInformationDetails")
            .field("detailsTab_AdditionalDefendants")
            .field("detailsTab_DefendantCircumstanceDetails")
            .label(
                "Underlessee or mortgagee",
                "detailsTab_MortgageDetails!=\"\"",
                "## Underlessee or mortgagee entitled to claim relief against forfeiture"
            )
            .field("detailsTab_MortgageOneDetails")
            .field("detailsTab_MortgageDetails")
            .label(
                "Demotion of tenancy",
                "detailsTab_DemotionOfTenancyDetails!=\"\"",
                "## Demotion of tenancy"
            )
            .field("detailsTab_DemotionOfTenancyDetails")
            .label(
                "Suspension of right to buy",
                "detailsTab_SuspensionOfRightToBuyDetails!=\"\"",
                "## Suspension of right to buy"
            )
            .field("detailsTab_SuspensionOfRightToBuyDetails");
    }
}
