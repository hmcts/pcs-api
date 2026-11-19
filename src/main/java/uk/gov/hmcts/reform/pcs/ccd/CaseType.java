package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.bundling.CaseBundleDefinition;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.util.Arrays;
import java.util.EnumSet;

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
    static final AccessProfile[] INTERNAL_TAB_ROLES = {
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

        builder.tabsRespectDeclaredFieldAccess();
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
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field(PCSCase::getCaseFileView, null, "#ARGUMENT(CaseFileView)");

        buildBundleTab(builder);

        buildSummaryTab(builder);

        builder.tab("CaseHistory", "History")
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field("caseHistory");

        builder.tab("hidden", "HiddenFields")
            .showCondition(NEVER_SHOW)
            .field(PCSCase::getCaseTitleMarkdown)
            .field(PCSCase::getDashboardData)
            .field(PCSCase::getCaseNameHmctsInternal)
            .field(PCSCase::getFeatureFlags);

        builder.tab("serviceRequest", "Service Request")
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field("waysToPay");

        buildCaseNotesTab(builder);

        builder.tab("caseLinks", "Linked Cases")
            .field(PCSCase::getLinkedCasesComponentLauncher, null, "#ARGUMENT(LinkedCases)")
            .field(PCSCase::getCaseLinks, "LinkedCasesComponentLauncher!=\"\"", "#ARGUMENT(LinkedCases)");

        builder.tab("caseFlags", "Case flags")
            .field(PCSCase::getFlagLauncherInternal, null, "#ARGUMENT(READ)")
            .field(PCSCase::getCaseFlags, "flagLauncherInternal!=\"\"")
            .field(PCSCase::getParties, "flagLauncherInternal!=\"\"", "#ARGUMENT(Flags)");

        buildSupportTab(builder);

        builder.tab("Messages", "Messages")
            .showCondition(ShowConditions.and(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS),
                ShowConditions.featureFlagsEnabled(FeatureFlag.MAKE_ORDER)))
            .field("messagesMarkdownLabel")
            .field("messagesMarkdown", NEVER_SHOW);

        buildJudicialNotesTab(builder);

        builder.tab("paymentHistory", "Payment History")
            .showCondition(ShowConditions.stateNotEquals(AWAITING_SUBMISSION_TO_HMCTS))
            .field(PCSCase::getCasePaymentHistoryViewer);

        if (shutterService) {
            builder.shutterService();
        }

        configureCaseFileCategories(builder);
    }

    private void configureCaseFileCategories(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        for (CaseFileCategory category : CaseFileCategory.values()) {
            builder.categories(AccessProfile.GA_CLAIMANT_SOLICITOR)
                .categoryID(category.getId())
                .categoryLabel(category.getLabel())
                .displayOrder(category.getDisplayOrder())
                .build();
        }
        builder.categories(AccessProfile.GA_CLAIMANT_SOLICITOR)
            .categoryID(CaseBundleDefinition.FOLDER_ID)
            .categoryLabel(CaseBundleDefinition.FOLDER_LABEL)
            .displayOrder(CaseFileCategory.values().length + 1)
            .build();
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
            .field("caseReviewDatesMarkdownLabel")
            .field("caseReviewDatesMarkdown", NEVER_SHOW)
            .field(PCSCase::getCaseNotes);
    }

    private void buildBundleTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("bundle", "Bundle")
            .showCondition("bundleTab_Status=\"*\"")
            .field("bundleTab_Status")
            .field("bundleTab_Bundle")
            .field("bundleTab_LastUpdated")
            .field("bundleTab_Pages")
            .field("bundleTab_MissingDocuments");
    }

    private void buildCasePartiesTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("caseParties", "Case Parties")
            .field("casePartiesHeading")
            .field("casePartiesTab_ClaimantDetails")
            .field("casePartiesTab_ClaimantsDetails")
            .field("casePartiesTab_DefendantOneDetails")
            .field("casePartiesTab_DefendantsDetails")
            .field("casePartiesTab_LfDetails")
            .field("casePartiesTab_LfsDetails");
    }

    private void buildSummaryTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("summary", "Summary")
            .field("summaryLegalRepresentativeMarkdownLabel")
            .field("summaryLegalRepresentativeMarkdown", NEVER_SHOW)
            .field("confirmEvictionSummaryMarkupLabel")
            .field("confirmEvictionSummaryMarkup", NEVER_SHOW)
            .field("summaryHeading")
            .field("summaryTab_RepossessedPropertyAddress")
            .field("summaryTab_GroundsForPossession")
            .field("summaryTab_ReasonsForPossession")
            .field("summaryTab_DateClaimSubmitted")
            .field("claimantDetailsHeading", "summaryTab_ClaimantDetails!=\"\"")
            .field("summaryTab_ClaimantDetails")
            .field("defendantDetailsHeading", "summaryTab_DefendantDetails!=\"\"")
            .field("summaryTab_DefendantDetails")
            .field("summaryTab_AdditionalDefendants")
            .field("rentArrearsHeading", "summaryTab_RentArrearsDetails!=\"\"")
            .field("summaryTab_RentArrearsDetails")
            .field("tenancyOrOccupationContractOrLicenceHeading", "summaryTab_TenancyDetails!=\"\"")
            .field("summaryTab_TenancyDetails")
            .field("occupationContractOrLicenceHeading", "summaryTab_OccupationContractOrLicenceDetails!=\"\"")
            .field("summaryTab_OccupationContractOrLicenceDetails")
            .field("noticeHeading", "summaryTab_NoticeDetails!=\"\"")
            .field("summaryTab_NoticeDetails")
            .field("summaryTab_BreathingSpaceDetails", "summaryTab_BreathingSpaceDetails!=\"\"");
    }

    private void buildCaseDetailsTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("caseDetails", "Case Details")
            .field("caseDetailsHeading")
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
            .field("claimantDetailsHeading", "detailsTab_ClaimantInformation!=\"\"")
            .field("detailsTab_ClaimantInformation")
            .field("detailsTab_ClaimantAddress")
            .field("detailsTab_ClaimantContactDetails")
            .field("detailsTab_ClaimantRegistrationAndLicensingDetails")
            .field("detailsTab_ClaimantCircumstances")
            .field("defendantDetailsHeading", "detailsTab_DefendantInformationDetails!=\"\"")
            .field("detailsTab_DefendantInformationDetails")
            .field("detailsTab_AdditionalDefendants")
            .field("detailsTab_DefendantCircumstanceDetails")
            .field("underlesseeOrMortgageeHeading", "detailsTab_MortgageDetails!=\"\"")
            .field("detailsTab_MortgageOneDetails")
            .field("detailsTab_MortgageDetails")
            .field("demotionOfTenancyHeading", "detailsTab_DemotionOfTenancyDetails!=\"\"")
            .field("detailsTab_DemotionOfTenancyDetails")
            .field("suspensionOfRightToBuyHeading", "detailsTab_SuspensionOfRightToBuyDetails!=\"\"")
            .field("detailsTab_SuspensionOfRightToBuyDetails")
            .field("prohibitedConductStandardContractHeading",
                   "detailsTab_ProhibitedConductStandardContractDetails!=\"\"")
            .field("detailsTab_ProhibitedConductStandardContractDetails")
            .field("requiredDocumentsHeading", "detailsTab_RequiredDocumentsDetails!=\"\"")
            .field("detailsTab_RequiredDocumentsDetails")
            .field("documentsYouveUploadedHeading", "detailsTab_UploadedDocumentsChecklistDetails!=\"\"")
            .field("detailsTab_UploadedDocumentsChecklistDetails");
    }

    private void buildCaseListView(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.workBasketResultFields()
            .field("[CASE_REFERENCE]", "Case number")
            .field(PCSCase::getClaimIssueDate, "Date issued")
            .field(PCSCase::getClaimantNames, "Claimant names")
            .field(PCSCase::getDefendantNames, "Defendant names")
            .field(PCSCase::getPostCode, "Postcode")
            .field("[STATE]", "State");
    }

    private void buildJudicialNotesTab(ConfigBuilder<PCSCase, State, AccessProfile> builder) {
        builder.tab("judicialNotes", "Judicial notes")
            .field("judicialNotesMarkdownLabel")
            .field("judicialNotesMarkdown", NEVER_SHOW);
    }
}
