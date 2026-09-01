package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.api.AccessType;
import uk.gov.hmcts.ccd.sdk.api.AccessTypeRole;
import uk.gov.hmcts.ccd.sdk.api.CaseCategory;
import uk.gov.hmcts.ccd.sdk.api.PropertyUtils;
import uk.gov.hmcts.ccd.sdk.api.Search;
import uk.gov.hmcts.ccd.sdk.api.SearchCases;
import uk.gov.hmcts.ccd.sdk.api.Tab;
import uk.gov.hmcts.ccd.sdk.api.Tab.TabBuilder;
import uk.gov.hmcts.ccd.sdk.api.TabField;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseTypeTest {

    @InjectMocks
    private CaseType caseType;

    @Mock
    private ConfigBuilderImpl<PCSCase, State, AccessProfile> builder;

    @Mock
    private PropertyUtils utils;

    @Test
    void shouldGetCaseType() {
        // When
        String caseTyp = CaseType.getCaseType();

        // Then
        assertThat(caseTyp).contains("PCS");
    }

    @Test
    void shouldNotBeSuffixedCaseTypeWhenSuffixAbsentOrBlank() {
        // Canonical PCS (indexed into global search): unset or blank CASE_TYPE_SUFFIX.
        // The pipeline encodes "canonical" as either unset or an empty string (see Jenkinsfile_CNP).
        assertThat(CaseType.isSuffixed(null)).isFalse();
        assertThat(CaseType.isSuffixed("")).isFalse();
        assertThat(CaseType.isSuffixed("   ")).isFalse();
    }

    @Test
    void shouldBeSuffixedCaseTypeWhenSuffixSet() {
        // Suffixed (e.g. PCS-STAGING, PR previews) -> NOT indexed into global search.
        assertThat(CaseType.isSuffixed("staging")).isTrue();
        assertThat(CaseType.isSuffixed("1234")).isTrue();
    }

    @Test
    void shouldGetJurisdictionId() {
        // When
        String jurisdictionId = CaseType.getJurisdictionId();

        // Then
        assertThat(jurisdictionId).contains("PCS");
    }

    @Test
    void shouldGetCaseTypeName() {
        // When
        String caseTypeName = CaseType.getCaseTypeName();

        // Then
        assertThat(caseTypeName).contains("Possession");
    }

    @Test
    void shouldConfigureCaseTypeTabs() {
        // Given
        final TabBuilder<PCSCase, AccessProfile> nextStepsTabBuilder = TabBuilder.builder(PCSCase.class, utils);
        final TabBuilder<PCSCase, AccessProfile> summaryTabBuilder = TabBuilder.builder(PCSCase.class, utils);
        final TabBuilder<PCSCase, AccessProfile> caseHistoryTabBuilder = TabBuilder.builder(PCSCase.class, utils);
        final TabBuilder<PCSCase, AccessProfile> hiddenTabBuilder = TabBuilder.builder(PCSCase.class, utils);
        final TabBuilder<PCSCase, AccessProfile> serviceRequestTabBuilder = TabBuilder.builder(PCSCase.class, utils);
        final Tab.TabBuilder<PCSCase, AccessProfile> caseNotesTabBuilder = Tab.TabBuilder.builder(PCSCase.class, utils);
        final TabBuilder<PCSCase, AccessProfile> caseLinksTabBuilder = TabBuilder.builder(PCSCase.class, utils);
        final TabBuilder<PCSCase, AccessProfile> caseFileViewTabBuilder = TabBuilder.builder(PCSCase.class, utils);
        final TabBuilder<PCSCase, AccessProfile> casePartiesTabBuilder = TabBuilder.builder(PCSCase.class, utils);
        final Tab.TabBuilder<PCSCase, AccessProfile> caseFlagsTabBuilder = Tab.TabBuilder.builder(PCSCase.class, utils);
        final Tab.TabBuilder<PCSCase, AccessProfile> supportTabBuilder = Tab.TabBuilder.builder(PCSCase.class, utils);
        final Tab.TabBuilder<PCSCase, AccessProfile> caseDetailsTabBuilder =
            Tab.TabBuilder.builder(PCSCase.class, utils);
        final Search.SearchBuilder<PCSCase, AccessProfile> searchBuilder =
            Search.SearchBuilder.builder(PCSCase.class, utils);
        final SearchCases.SearchCasesBuilder<PCSCase> searchCasesBuilder =
            SearchCases.SearchCasesBuilder.builder(PCSCase.class, utils);
        final AccessType.AccessTypeBuilder accessTypeBuilder =
            AccessType.AccessTypeBuilder.builder("accessTypeId");
        final AccessTypeRole.AccessTypeRoleBuilder accessTypeRoleBuilder =
            AccessTypeRole.AccessTypeRoleBuilder.builder("accessTypeId");

        when(builder.searchInputFields()).thenReturn(searchBuilder);
        when(builder.searchCasesFields()).thenReturn(searchCasesBuilder);
        when(builder.searchResultFields()).thenReturn(searchBuilder);
        when(builder.workBasketResultFields()).thenReturn(searchBuilder);
        when(builder.tab("nextSteps", "Next steps")).thenReturn(nextStepsTabBuilder);
        when(builder.tab("summary", "Summary")).thenReturn(summaryTabBuilder);
        when(builder.tab("CaseHistory", "History")).thenReturn(caseHistoryTabBuilder);
        when(builder.tab("hidden", "HiddenFields")).thenReturn(hiddenTabBuilder);
        when(builder.tab("serviceRequest", "Service Request")).thenReturn(serviceRequestTabBuilder);
        when(builder.tab("notes", "Notes")).thenReturn(caseNotesTabBuilder);
        when(builder.tab("caseLinks", "Linked Cases")).thenReturn(caseLinksTabBuilder);
        when(builder.tab("caseFileView", "Case File View")).thenReturn(caseFileViewTabBuilder);
        when(builder.tab("caseParties", "Case Parties")).thenReturn(casePartiesTabBuilder);
        when(builder.tab("caseFlags", "Case flags")).thenReturn(caseFlagsTabBuilder);
        when(builder.tab("support", "Support")).thenReturn(supportTabBuilder);
        when(builder.tab("caseDetails", "Case Details")).thenReturn(caseDetailsTabBuilder);
        when(builder.categories(AccessProfile.GA_CLAIMANT_SOLICITOR))
            .thenReturn(CaseCategory.CaseCategoryBuilder.builder(AccessProfile.GA_CLAIMANT_SOLICITOR));
        lenient().when(builder.accessType(anyString())).thenReturn(accessTypeBuilder);
        lenient().when(builder.accessTypeRole(anyString())).thenReturn(accessTypeRoleBuilder);

        // When
        caseType.configure(builder);
        final Tab<PCSCase, AccessProfile> nextStepsTab = nextStepsTabBuilder.build();
        final Tab<PCSCase, AccessProfile> summaryTab = summaryTabBuilder.build();
        final Tab<PCSCase, AccessProfile> caseHistoryTab = caseHistoryTabBuilder.build();
        final Tab<PCSCase, AccessProfile> hiddenTab = hiddenTabBuilder.build();
        final Tab<PCSCase, AccessProfile> serviceRequestTab = serviceRequestTabBuilder.build();
        final Tab<PCSCase, AccessProfile> caseLinksTab = caseLinksTabBuilder.build();
        final Tab<PCSCase, AccessProfile> casePartiesTab = casePartiesTabBuilder.build();
        final Tab<PCSCase, AccessProfile> caseFileViewTab = caseFileViewTabBuilder.build();
        final Tab<PCSCase, AccessProfile> caseDetailsTab = caseDetailsTabBuilder.build();
        final Tab<PCSCase, AccessProfile> caseNotesTab = caseNotesTabBuilder.build();
        final Tab<PCSCase, AccessProfile> caseFlagsTab = caseFlagsTabBuilder.build();
        final Tab<PCSCase, AccessProfile> supportTab = supportTabBuilder.build();


        // Then
        assertThat(nextStepsTab.getFields()).extracting(TabField::getId).contains("nextStepsMarkdown");
        assertThat(summaryTab.getFields()).extracting(TabField::getId).contains("confirmEvictionSummaryMarkup");
        assertThat(caseHistoryTab.getFields()).extracting(TabField::getId).contains("caseHistory");
        assertThat(hiddenTab.getFields()).hasSize(4);
        assertThat(serviceRequestTab.getFields()).extracting(TabField::getId).contains("waysToPay");
        assertThat(caseLinksTab.getFields()).extracting(TabField::getShowCondition)
            .contains("LinkedCasesComponentLauncher!=\"\"");
        assertThat(caseFileViewTab.getFields()).hasSize(1);
        assertThat(casePartiesTab.getFields()).extracting(TabField::getId).contains("casePartiesTab_ClaimantDetails");
        assertThat(caseDetailsTab.getFields()).extracting(TabField::getId).contains("detailsTab_ClaimDetails");
        assertThat(summaryTab.getFields()).extracting(TabField::getId)
            .contains("summaryTab_OccupationContractOrLicenceDetails");
        assertThat(summaryTab.getForRoles()).containsExactlyInAnyOrder(CaseType.PARTY_VISIBLE_TAB_ROLES);
        assertThat(casePartiesTab.getForRoles()).containsExactlyInAnyOrder(CaseType.PARTY_VISIBLE_TAB_ROLES);
        assertThat(caseDetailsTab.getForRoles()).containsExactlyInAnyOrder(CaseType.PARTY_VISIBLE_TAB_ROLES);
        assertThat(caseFileViewTab.getForRoles()).containsExactlyInAnyOrder(CaseType.PARTY_VISIBLE_TAB_ROLES);
        assertThat(serviceRequestTab.getForRoles()).containsExactlyInAnyOrder(CaseType.PARTY_VISIBLE_TAB_ROLES);
        assertThat(caseHistoryTab.getForRoles()).containsExactlyInAnyOrder(CaseType.INTERNAL_TAB_ROLES);
        assertThat(caseLinksTab.getForRoles()).containsExactlyInAnyOrder(CaseType.INTERNAL_TAB_ROLES);
        assertThat(caseNotesTab.getForRoles()).containsExactlyInAnyOrder(CaseType.INTERNAL_TAB_ROLES);
        assertThat(caseFlagsTab.getForRoles()).containsExactlyInAnyOrder(CaseType.INTERNAL_TAB_ROLES);
        assertThat(supportTab.getForRoles()).containsExactlyInAnyOrder(CaseType.EXTERNAL_FLAG_TAB_ROLES);
        assertThat(supportTab.getForRoles())
            .contains(AccessProfile.CLAIMANT, AccessProfile.GA_CLAIMANT_SOLICITOR,
                      AccessProfile.GA_DEFENDANT_SOLICITOR)
            .doesNotContain(AccessProfile.CLAIMANT_SOLICITOR, AccessProfile.DEFENDANT_SOLICITOR);
        // A professional whose access to a case comes through their organisation's group access holds the
        // group access profile rather than a case role, so the Support tab has to be granted to those
        // profiles or it is absent for them. Which party's support the tab shows stays filtered per party.
        assertThat(supportTab.getForRoles())
            .contains(AccessProfile.CLAIMANT,
                      AccessProfile.GA_CLAIMANT_SOLICITOR,
                      AccessProfile.GA_DEFENDANT_SOLICITOR);
        // One tab definition is generated per access profile, each with its own tab id and the same
        // label, so a profile listed twice would render the tab twice.
        assertThat(supportTab.getForRoles()).doesNotHaveDuplicates();
        verify(builder).omitHistoryForRoles(CaseType.NON_INTERNAL_HISTORY_ROLES);

        assertThat(supportTab.getFields()).hasSize(2);
        assertThat(supportTab.getFields()).extracting(TabField::getDisplayContextParameter)
            .containsExactly("#ARGUMENT(READ,EXTERNAL)", "#ARGUMENT(Flags)");
        assertThat(supportTab.getFields()).extracting(TabField::getShowCondition)
            .containsExactly(null, "flagLauncherExternal!=\"\"");
        assertThat(supportTab.getShowCondition())
            .isEqualTo("[STATE]!=\"AWAITING_SUBMISSION_TO_HMCTS\"");
    }

    /**
     * The config generator writes one CCD tab definition per access profile, giving each its own tab id
     * while they all share the tab label. A profile repeated within a role set therefore renders the tab
     * more than once for a user holding it.
     */
    @Test
    void shouldNotRepeatAnAccessProfileWithinATabRoleSet() {
        assertThat(CaseType.EXTERNAL_FLAG_TAB_ROLES).doesNotHaveDuplicates();
        assertThat(CaseType.PARTY_VISIBLE_TAB_ROLES).doesNotHaveDuplicates();
        assertThat(CaseType.INTERNAL_TAB_ROLES).doesNotHaveDuplicates();
    }

    /**
     * Support is a party-visible tab, so every external profile that can reach a case has to be able to
     * reach it. Internal-only profiles stay off it: internal users review support through Case flags.
     */
    @Test
    void shouldGrantTheSupportTabToEveryExternalPartyVisibleProfile() {
        List<AccessProfile> externalPartyVisible = Arrays.stream(CaseType.PARTY_VISIBLE_TAB_ROLES)
            .filter(profile -> !Arrays.asList(CaseType.INTERNAL_TAB_ROLES).contains(profile))
            .toList();

        assertThat(CaseType.EXTERNAL_FLAG_TAB_ROLES).containsAll(externalPartyVisible);
        assertThat(CaseType.EXTERNAL_FLAG_TAB_ROLES).doesNotContainAnyElementsOf(
            Arrays.asList(CaseType.INTERNAL_TAB_ROLES));
    }

    @Test
    void shouldShutterServiceWhenShutterFlagEnabled() {
        // Given
        stubBuilderForConfigure();
        ReflectionTestUtils.setField(caseType, "shutterService", true);

        // When
        caseType.configure(builder);

        // Then
        verify(builder).shutterService();
    }

    @Test
    void shouldNotShutterServiceWhenShutterFlagDisabled() {
        // Given
        stubBuilderForConfigure();
        ReflectionTestUtils.setField(caseType, "shutterService", false);

        // When
        caseType.configure(builder);

        // Then
        verify(builder, never()).shutterService();
    }

    private void stubBuilderForConfigure() {
        final Search.SearchBuilder<PCSCase, AccessProfile> searchBuilder =
            Search.SearchBuilder.builder(PCSCase.class, utils);
        final SearchCases.SearchCasesBuilder<PCSCase> searchCasesBuilder =
            SearchCases.SearchCasesBuilder.builder(PCSCase.class, utils);

        when(builder.searchInputFields()).thenReturn(searchBuilder);
        when(builder.searchCasesFields()).thenReturn(searchCasesBuilder);
        when(builder.searchResultFields()).thenReturn(searchBuilder);
        when(builder.workBasketResultFields()).thenReturn(searchBuilder);
        lenient().when(builder.accessType(anyString()))
            .thenReturn(AccessType.AccessTypeBuilder.builder("accessTypeId"));
        lenient().when(builder.accessTypeRole(anyString()))
            .thenReturn(AccessTypeRole.AccessTypeRoleBuilder.builder("accessTypeId"));
        when(builder.tab("nextSteps", "Next steps")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.tab("summary", "Summary")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.tab("CaseHistory", "History")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.tab("hidden", "HiddenFields")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.tab("serviceRequest", "Service Request")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.tab("notes", "Notes")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.tab("caseLinks", "Linked Cases")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.tab("caseFileView", "Case File View")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.tab("caseParties", "Case Parties")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.tab("caseFlags", "Case flags")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.tab("support", "Support")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.tab("caseDetails", "Case Details")).thenReturn(TabBuilder.builder(PCSCase.class, utils));
        when(builder.categories(AccessProfile.GA_CLAIMANT_SOLICITOR))
            .thenReturn(CaseCategory.CaseCategoryBuilder.builder(AccessProfile.GA_CLAIMANT_SOLICITOR));
    }
}
