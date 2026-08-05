package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static java.lang.System.getenv;
import static uk.gov.hmcts.ccd.sdk.api.Permission.CRU;
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
    static final AccessProfile[] PARTY_VISIBLE_TAB_ROLES = {
        AccessProfile.CITIZEN,
        AccessProfile.DEFENDANT,
        AccessProfile.PCS_SOLICITOR,
        AccessProfile.JUDGE,
        AccessProfile.FEE_PAID_JUDGE,
        AccessProfile.CIRCUIT_JUDGE,
        AccessProfile.LEADERSHIP_JUDGE,
        AccessProfile.HEARING_CENTRE_ADMIN,
        AccessProfile.CTSC_ADMIN,
        AccessProfile.WLU_ADMIN
    };
    static final AccessProfile[] INTERNAL_TAB_ROLES = {
        AccessProfile.JUDGE,
        AccessProfile.FEE_PAID_JUDGE,
        AccessProfile.CIRCUIT_JUDGE,
        AccessProfile.LEADERSHIP_JUDGE,
        AccessProfile.HEARING_CENTRE_ADMIN,
        AccessProfile.CTSC_ADMIN,
        AccessProfile.WLU_ADMIN
    };
    static final AccessProfile[] NON_INTERNAL_HISTORY_ROLES = nonInternalHistoryRoles();

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

        configureGaAccessTypes(builder);

        // State ACLs for the group-access capacities, else the data store filters matched cases out.
        for (State state : State.values()) {
            builder.grant(state, CRU, AccessProfile.CLAIMANT_ORG);
            builder.grant(state, CRU, AccessProfile.CLAIMANT_SOLICITOR_ORG);
            builder.grant(state, CRU, AccessProfile.DEFENDANT_SOLICITOR_ORG);
        }

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

        if (shutterService) {
            builder.shutterService();
        }

        configureCaseFileCategories(builder);
    }

    // Organisations that are themselves the claimant - councils, housing associations and the
    // "other" categories. A solicitor firm is never here; it represents a party rather than being one.
    private static final List<String> CLAIMANT_ORG_PROFILES = List.of(
        "LOCALAUTH_PROFILE",
        "OTHER_REALT_PROFILE",
        "OTHER_PROP_PROFILE",
        "OTHER_NFP_PROFILE",
        "OTHER_CHARITY_PROFILE"
    );

    private static final String SOLICITOR_PROFILE = "SOLICITOR_PROFILE";
    private static final String LIVE_TO = "01/01/2027";

    // Substituted by CCD with the organisation id from the matching organisation policy.
    public static final String ORG_ID_PLACEHOLDER = "$ORGID$";

    // The group role name is the capacity itself, matching what POFCC-368 registers in the RAS
    // catalogue: claimant is common to every organisation that IS the claimant regardless of org
    // profile, while claimant_solicitor / defendant_solicitor are for organisations representing one.

    private static void configureGaAccessTypes(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        // Def store rejects the import unless every AccessType row has a unique DisplayOrder,
        // so the counter runs across every row rather than restarting per access type.
        int displayOrder = 1;

        // Case creation has no case to evaluate against, so it is authorised by the plain
        // organisational role. POFCC-368 registers claimant with caseAccessGroupId optional for
        // exactly this: the same role name, held without a group id.
        builder.accessType("create-cases")
            .organisationProfileId("LOCALAUTH_PROFILE")
            .accessMandatory(true)
            .accessDefault(true)
            .display(false)
            .hintText("Access to create cases")
            .displayOrder(displayOrder++)
            .liveTo(LIVE_TO);

        builder.accessTypeRole("create-cases")
            .organisationProfileId("LOCALAUTH_PROFILE")
            .organisationalRoleName("claimant")
            .liveTo(LIVE_TO);

        // A solicitor firm can act on either side, and on opposite sides of different cases, so the
        // two capacities are separate options for the organisation's admin to assign per user. One
        // name per capacity, everywhere: case role, group role and access profile are all the name
        // POFCC-368 registers in the RAS catalogue.
        displayOrder = addSolicitorAccessType(builder, "solicitor-org-claimant-access",
            "claimant_solicitor", "claimant", displayOrder);
        displayOrder = addSolicitorAccessType(builder, "solicitor-org-defendant-access",
            "defendant_solicitor", "defendant", displayOrder);

        for (String orgProfile : CLAIMANT_ORG_PROFILES) {
            builder.accessType("prof-org-claimant-access")
                .organisationProfileId(orgProfile)
                .accessMandatory(true)
                .accessDefault(false)
                .display(true)
                .description("Can manage all cases associated with this organisation as claimant")
                .hintText("Assign to Users to enable access to all cases associated with this organisation")
                .displayOrder(displayOrder++)
                .liveTo(LIVE_TO);

            builder.accessTypeRole("prof-org-claimant-access")
                .organisationProfileId(orgProfile)
                .groupRoleName("claimant")
                .caseAssignedRoleField("claimant")
                .groupAccessEnabled(true)
                .caseAccessGroupIdTemplate("PCS:PCS:prof-org-claimant-access:claimant:" + ORG_ID_PLACEHOLDER)
                .liveTo(LIVE_TO);
        }
    }

    private static int addSolicitorAccessType(ConfigBuilder<PCSCase, State, AccessProfile> builder,
                                              String accessTypeId,
                                              String role,
                                              String party,
                                              int displayOrder) {
        builder.accessType(accessTypeId)
            .organisationProfileId(SOLICITOR_PROFILE)
            .accessMandatory(true)
            .accessDefault(false)
            .display(true)
            .description("Can manage all cases associated with this organisation as " + party)
            .hintText("Assign to Users to enable access to all cases associated with this organisation")
            .displayOrder(displayOrder)
            .liveTo(LIVE_TO);

        builder.accessTypeRole(accessTypeId)
            .organisationProfileId(SOLICITOR_PROFILE)
            .groupRoleName(role)
            .caseAssignedRoleField(role)
            .groupAccessEnabled(true)
            .caseAccessGroupIdTemplate("PCS:PCS:" + accessTypeId + ":" + role + ":" + ORG_ID_PLACEHOLDER)
            .liveTo(LIVE_TO);

        return displayOrder + 1;
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
            .forRoles(INTERNAL_TAB_ROLES)
            .field(PCSCase::getCaseNotes);
    }

    private void buildCasePartiesTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("caseParties", "Case Parties")
            .forRoles(PARTY_VISIBLE_TAB_ROLES)
            .label("Case parties", null, "# Case Parties")
            .field("casePartiesTab_ClaimantDetails")
            .field("casePartiesTab_DefendantOneDetails")
            .field("casePartiesTab_DefendantsDetails");
    }

    private void buildSummaryTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("summary", "Summary")
            .forRoles(PARTY_VISIBLE_TAB_ROLES)
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
            .field("detailsTab_RequiredDocumentsDetails");
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
