package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.Arrays;
import java.util.EnumSet;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

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
    static final AccessProfile[] PARTY_VISIBLE_TAB_ROLES = {
        AccessProfile.CITIZEN,
        AccessProfile.DEFENDANT,
        AccessProfile.GA_DEFENDANT_SOLICITOR,
        AccessProfile.CLAIMANT,
        AccessProfile.PCS_SOLICITOR,
        AccessProfile.GA_CLAIMANT_SOLICITOR,
        AccessProfile.JUDGE,
        AccessProfile.FEE_PAID_JUDGE,
        AccessProfile.CIRCUIT_JUDGE,
        AccessProfile.LEADERSHIP_JUDGE,
        AccessProfile.HEARING_CENTRE_ADMIN,
        AccessProfile.CTSC_ADMIN,
        AccessProfile.WLU_ADMIN,
        AccessProfile.BAILIFF,
        AccessProfile.BAILIFF_ADMIN,
        AccessProfile.BAILIFF_MANAGER
    };
    static final AccessProfile[] INTERNAL_TAB_ROLES = {
        AccessProfile.JUDGE,
        AccessProfile.FEE_PAID_JUDGE,
        AccessProfile.CIRCUIT_JUDGE,
        AccessProfile.LEADERSHIP_JUDGE,
        AccessProfile.HEARING_CENTRE_ADMIN,
        AccessProfile.CTSC_ADMIN,
        AccessProfile.WLU_ADMIN,
        AccessProfile.BAILIFF,
        AccessProfile.BAILIFF_ADMIN,
        AccessProfile.BAILIFF_MANAGER
    };

    static final AccessProfile[] CASE_NOTE_TAB_ROLES = {
        AccessProfile.JUDGE,
        AccessProfile.FEE_PAID_JUDGE,
        AccessProfile.CIRCUIT_JUDGE,
        AccessProfile.LEADERSHIP_JUDGE,
        AccessProfile.HEARING_CENTRE_ADMIN,
        AccessProfile.CTSC_ADMIN,
        AccessProfile.WLU_ADMIN
    };

    static final AccessProfile[] DEFENDANT_SUPPORT_TAB_ROLES = {
        AccessProfile.CITIZEN,
        AccessProfile.DEFENDANT,
        AccessProfile.GA_DEFENDANT_SOLICITOR
    };

    static final AccessProfile[] NON_INTERNAL_HISTORY_ROLES = nonInternalHistoryRoles();

    static final AccessProfile[] PAYMENT_HISTORY_TAB_ROLES = {
        AccessProfile.JUDGE,
        AccessProfile.FEE_PAID_JUDGE,
        AccessProfile.CIRCUIT_JUDGE,
        AccessProfile.LEADERSHIP_JUDGE,
        AccessProfile.HEARING_CENTRE_TEAM_LEADER,
        AccessProfile.HEARING_CENTRE_ADMIN,
        AccessProfile.CTSC_TEAM_LEADER,
        AccessProfile.CTSC_ADMIN,
        AccessProfile.WLU_TEAM_LEADER,
        AccessProfile.WLU_ADMIN
    };

    /**
     * Internal users and the claimant side. Defendants are left out until the notice of eviction can be marked
     * as served (HDPI-7346), which is when they may see a warrant; they never see writs.
     */
    static final AccessProfile[] ENFORCEMENT_STATUS_TAB_ROLES = {
        AccessProfile.JUDGE,
        AccessProfile.FEE_PAID_JUDGE,
        AccessProfile.CIRCUIT_JUDGE,
        AccessProfile.LEADERSHIP_JUDGE,
        AccessProfile.HEARING_CENTRE_ADMIN,
        AccessProfile.CTSC_ADMIN,
        AccessProfile.WLU_ADMIN,
        AccessProfile.BAILIFF,
        AccessProfile.BAILIFF_ADMIN,
        AccessProfile.BAILIFF_MANAGER,
        AccessProfile.CLAIMANT,
        AccessProfile.PCS_SOLICITOR,
        AccessProfile.GA_CLAIMANT_SOLICITOR
    };

    @Value("${hmcts.hmctsOrgId}")
    private String hmctsServiceId;

    @Value("${caseApi.url}")
    private String caseApiUrl;

    @Value("${shutter.service:false}")
    private boolean shutterService;

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

    private static AccessProfile[] nonInternalHistoryRoles() {
        EnumSet<AccessProfile> internalRoles = EnumSet.copyOf(Arrays.asList(INTERNAL_TAB_ROLES));

        return Arrays.stream(AccessProfile.values())
            .filter(accessProfile -> !internalRoles.contains(accessProfile))
            .toArray(AccessProfile[]::new);
    }

    /**
     * Whether this deployment runs a suffixed case type, e.g. PCS-STAGING or a
     * per-PR preview type, driven by the CASE_TYPE_SUFFIX env var.
     */
    public static boolean isSuffixedCaseType() {
        return isSuffixed(getenv().get("CASE_TYPE_SUFFIX"));
    }

    static boolean isSuffixed(String suffix) {
        return suffix != null && !suffix.isBlank();
    }

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.setCallbackHost(caseApiUrl);

        builder.caseType(getCaseType(), getCaseTypeName(), CASE_TYPE_DESCRIPTION);
        builder.jurisdiction(JURISDICTION_ID, JURISDICTION_NAME, JURISDICTION_DESCRIPTION);
        builder.hmctsServiceId(hmctsServiceId);

        builder.searchInputFields()
            .caseReferenceField();

        builder.omitHistoryForRoles(NON_INTERNAL_HISTORY_ROLES);

        builder.searchCasesFields()
            .caseReferenceField();

        builder.searchResultFields()
            .caseReferenceField();

        buildCaseListView(builder);

        builder.tab("nextSteps", "Next steps")
            .showCondition(ShowConditions.stateEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .label("nextStepsMarkdownLabel", null, "${nextStepsMarkdown}")
            .field("nextStepsMarkdown", NEVER_SHOW);

        buildCasePartiesTab(builder);

        buildCaseDetailsTab(builder);

        builder.tab("caseFileView", "Case File View")
            .forRoles(PARTY_VISIBLE_TAB_ROLES)
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field(PCSCase::getCaseFileView, null, "#ARGUMENT(CaseFileView)");

        buildSummaryTab(builder);

        builder.tab("CaseHistory", "History")
            .forRoles(INTERNAL_TAB_ROLES)
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field("caseHistory");

        builder.tab("hidden", "HiddenFields")
            .showCondition(NEVER_SHOW)
            .field(PCSCase::getCaseTitleMarkdown)
            .field(PCSCase::getDashboardData)
            .field(PCSCase::getCaseNameHmctsInternal)
            .field(PCSCase::getFeatureFlags);

        builder.tab("serviceRequest", "Service Request")
            .forRoles(PARTY_VISIBLE_TAB_ROLES)
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field("waysToPay");

        buildCaseNotesTab(builder);

        builder.tab("caseLinks", "Linked Cases")
            .forRoles(INTERNAL_TAB_ROLES)
            .field(PCSCase::getLinkedCasesComponentLauncher, null, "#ARGUMENT(LinkedCases)")
            .field(PCSCase::getCaseLinks, "LinkedCasesComponentLauncher!=\"\"", "#ARGUMENT(LinkedCases)");

        builder.tab("caseFlags", "Case flags")
            .forRoles(INTERNAL_TAB_ROLES)
            .field(PCSCase::getFlagLauncherInternal, null, "#ARGUMENT(READ)")
            .field(PCSCase::getCaseFlags, "flagLauncherInternal!=\"\"")
            .field(PCSCase::getParties, "flagLauncherInternal!=\"\"", "#ARGUMENT(Flags)");

        buildSupportTab(builder);

        builder.tab("paymentHistory", "Payment History")
            .forRoles(PAYMENT_HISTORY_TAB_ROLES)
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field(PCSCase::getCasePaymentHistoryViewer);

        buildEnforcementStatusTab(builder);
        buildRiskAssessmentTab(builder);

        if (shutterService) {
            builder.shutterService();
        }

        configureCaseFileCategories(builder);
    }

    private void configureCaseFileCategories(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        for (CaseFileCategory category : CaseFileCategory.values()) {
            addCaseFileCategory(builder, category.getId(), category.getLabel(), category.getDisplayOrder());
        }
        for (FeatureCaseFileCategory category : FeatureCaseFileCategory.values()) {
            addCaseFileCategory(builder, category.getId(), category.getLabel(), category.getDisplayOrder());
        }
    }

    // The SDK asks for a role but does not write it to Categories.json: CCD categories have no role
    // restriction, so everyone who can see Case File View sees every folder, even an empty one.
    private void addCaseFileCategory(ConfigBuilder<PCSCase, State, AccessProfile> builder,
                                     String id, String label, int displayOrder) {
        builder.categories(AccessProfile.GA_CLAIMANT_SOLICITOR)
            .categoryID(id)
            .categoryLabel(label)
            .displayOrder(displayOrder)
            .build();
    }

    /**
     * Every warrant and writ on the case. The enforcement module fills the {@code enforcements} field for the
     * case view and restricts who can read it; the tab appears once the case has an enforcement.
     */
    private void buildEnforcementStatusTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("enforcementStatus", "Enforcement status")
            .forRoles(ENFORCEMENT_STATUS_TAB_ROLES)
            .showCondition(and(featureFlagsEnabled(ENFORCEMENT), "enforcements!=\"\""))
            .field("enforcements");
    }

    /**
     * Enforcement risk assessment (HDPI-7350), shown for an approved, live warrant and not for a writ or a
     * warrant still awaiting payment or review (13-risk-assessment-tab.feature). Only the bailiff notes
     * (HDPI-7593) are built so far. The fields belong to enforcement, so they are named rather than referenced;
     * enforcement sets showRiskAssessmentTab, because a show condition cannot apply the rule itself.
     */
    private void buildRiskAssessmentTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("riskAssessment", "Risk assessment")
            .forRoles(INTERNAL_TAB_ROLES)
            .showCondition(and(featureFlagsEnabled(ENFORCEMENT), "showRiskAssessmentTab=\"Yes\""))
            .field("showRiskAssessmentTab", NEVER_SHOW)
            .field("bailiffNotes")
            .field("currentRiskLevel")
            .field("currentRiskLevelReason");
    }

    private void buildSupportTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("support", "Support")
            .forRoles(DEFENDANT_SUPPORT_TAB_ROLES)
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field(PCSCase::getFlagLauncherExternal, null, "#ARGUMENT(READ,EXTERNAL)")
            .field(PCSCase::getPartySupport, "flagLauncherExternal!=\"\"", "#ARGUMENT(Flags)");
    }

    private void buildCaseNotesTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("notes", "Notes")
            .forRoles(CASE_NOTE_TAB_ROLES)
            .field(PCSCase::getCaseReviewDates)
            .field(PCSCase::getCaseNotes);
    }

    private void buildCasePartiesTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("caseParties", "Case Parties")
            .forRoles(PARTY_VISIBLE_TAB_ROLES)
            .label("Case parties", null, "# Case Parties")
            .field("casePartiesTab_ClaimantDetails")
            .field("casePartiesTab_ClaimantsDetails")
            .field("casePartiesTab_DefendantOneDetails")
            .field("casePartiesTab_DefendantsDetails")
            .field("casePartiesTab_LfDetails")
            .field("casePartiesTab_LfsDetails");
    }

    private void buildSummaryTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("summary", "Summary")
            .forRoles(PARTY_VISIBLE_TAB_ROLES)
            .label("summaryLegalRepresentativeMarkdownLabel", null,
                   "${summaryLegalRepresentativeMarkdown}")
            .field("summaryLegalRepresentativeMarkdown", NEVER_SHOW)
            .label("confirmEvictionSummaryMarkupLabel", null, "${confirmEvictionSummaryMarkup}")
            .field("confirmEvictionSummaryMarkup", NEVER_SHOW)
            .label("Summary", null, "# Summary")
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
            .label("Occupation contract or licence",
                   "summaryTab_OccupationContractOrLicenceDetails!=\"\"",
                   "## Occupation contract or licence")
            .field("summaryTab_OccupationContractOrLicenceDetails")
            .label("Notice",
                   "summaryTab_NoticeDetails!=\"\"",
                   "## Notice")
            .field("summaryTab_NoticeDetails");
    }

    private void buildCaseDetailsTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("caseDetails", "Case Details")
            .forRoles(PARTY_VISIBLE_TAB_ROLES)
            .label("Case details", null, "# Case details")
            .field("detailsTab_ClaimDetails")
            .field("detailsTab_PropertyAddress")
            .field("detailsTab_GroundsForPossessionDetails")
            .field("detailsTab_DateClaimSubmitted")
            .field("detailsTab_OccupationContractLicenceDetails")
            .field("detailsTab_TenancyLicenceDetails")
            .field("detailsTab_NoticeDetails")
            .field("detailsTab_ActionsTakenDetails")
            .field("detailsTab_RentArrearsDetails")
            .field("detailsTab_ReasonsForPossessionDetails")
            .field("detailsTab_AntisocialAndConductDetails")
            .field("detailsTab_ApplicationsDetails")
            .label(
                "Claimant details",
                "detailsTab_ClaimantInformation!=\"\"",
                "## Claimant details"
            )
            .field("detailsTab_ClaimantInformation")
            .field("detailsTab_ClaimantAddress")
            .field("detailsTab_ClaimantContactDetails")
            .field("detailsTab_ClaimantRegistrationAndLicensingDetails")
            .field("detailsTab_ClaimantCircumstances")
            .label(
                "Defendant details",
                "detailsTab_DefendantInformationDetails!=\"\"",
                "## Defendant details"
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
            .field("detailsTab_SuspensionOfRightToBuyDetails")
            .label(
                "Prohibited conduct standard contract",
                "detailsTab_ProhibitedConductStandardContractDetails!=\"\"",
                "## Prohibited conduct standard contract"
            )
            .field("detailsTab_ProhibitedConductStandardContractDetails")
            .label(
                "Required Documents",
                "detailsTab_RequiredDocumentsDetails!=\"\"",
                "## Required Documents"
            )
            .field("detailsTab_RequiredDocumentsDetails")
            .label(
                "Documents you've uploaded",
                "detailsTab_UploadedDocumentsChecklistDetails!=\"\"",
                "## Documents you've uploaded"
            )
            .field("detailsTab_UploadedDocumentsChecklistDetails");
    }

    private void buildCaseListView(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.workBasketResultFields()
            .field("[CASE_REFERENCE]", "Case number")
            .field(PCSCase::getDateIssuedString, "Date issued")
            .field(PCSCase::getClaimantNames, "Claimant names")
            .field(PCSCase::getDefendantNames, "Defendant names")
            .field(PCSCase::getPostCode, "Postcode")
            .field("[STATE]", "State");
    }
}
