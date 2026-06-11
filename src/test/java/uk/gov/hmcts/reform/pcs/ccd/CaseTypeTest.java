package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
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
        String caseType = CaseType.getCaseType();

        // Then
        assertThat(caseType).contains("PCS");
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
        final Tab.TabBuilder<PCSCase, AccessProfile> caseDetailsTabBuilder =
            Tab.TabBuilder.builder(PCSCase.class, utils);
        final Search.SearchBuilder<PCSCase, AccessProfile> searchBuilder =
            Search.SearchBuilder.builder(PCSCase.class, utils);
        final SearchCases.SearchCasesBuilder<PCSCase> searchCasesBuilder =
            SearchCases.SearchCasesBuilder.builder(PCSCase.class, utils);

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
        when(builder.tab("caseDetails", "Case Details")).thenReturn(caseDetailsTabBuilder);
        when(builder.categories(AccessProfile.PCS_SOLICITOR))
            .thenReturn(CaseCategory.CaseCategoryBuilder.builder(AccessProfile.PCS_SOLICITOR));

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


        // Then
        assertThat(nextStepsTab.getFields()).extracting(TabField::getId).contains("nextStepsMarkdown");
        assertThat(summaryTab.getFields()).extracting(TabField::getId).contains("confirmEvictionSummaryMarkup");
        assertThat(caseHistoryTab.getFields()).extracting(TabField::getId).contains("caseHistory");
        assertThat(hiddenTab.getFields().size()).isEqualTo(2);
        assertThat(serviceRequestTab.getFields()).extracting(TabField::getId).contains("waysToPay");
        assertThat(caseLinksTab.getFields()).extracting(TabField::getShowCondition)
            .contains("LinkedCasesComponentLauncher!=\"\"");
        assertThat(caseFileViewTab.getFields().size()).isEqualTo(1);
        assertThat(casePartiesTab.getFields()).extracting(TabField::getId).contains("casePartiesTab_ClaimantDetails");
        assertThat(caseDetailsTab.getFields()).extracting(TabField::getId).contains("detailsTab_ClaimDetails");
        assertThat(summaryTab.getFields()).extracting(TabField::getId)
            .contains("summaryTab_OccupationContractOrLicenceDetails");
        assertThat(summaryTab.getForRoles()).contains(
            AccessProfile.CREATOR,
            AccessProfile.CITIZEN,
            AccessProfile.DEFENDANT,
            AccessProfile.CLAIMANT_SOLICITOR,
            AccessProfile.DEFENDANT_SOLICITOR,
            AccessProfile.JUDGE,
            AccessProfile.HEARING_CENTRE_TEAM_LEADER,
            AccessProfile.HEARING_CENTRE_ADMIN,
            AccessProfile.CTSC_TEAM_LEADER,
            AccessProfile.CTSC_ADMIN,
            AccessProfile.WLU_TEAM_LEADER,
            AccessProfile.WLU_ADMIN
        );
        assertThat(casePartiesTab.getForRoles()).containsAll(summaryTab.getForRoles());
        assertThat(caseDetailsTab.getForRoles()).containsAll(summaryTab.getForRoles());
        assertThat(caseFileViewTab.getForRoles()).containsAll(summaryTab.getForRoles());
        assertThat(serviceRequestTab.getForRoles()).containsAll(summaryTab.getForRoles());
        assertThat(caseHistoryTab.getForRoles()).contains(
            AccessProfile.JUDGE,
            AccessProfile.HEARING_CENTRE_TEAM_LEADER,
            AccessProfile.HEARING_CENTRE_ADMIN,
            AccessProfile.CTSC_TEAM_LEADER,
            AccessProfile.CTSC_ADMIN,
            AccessProfile.WLU_TEAM_LEADER,
            AccessProfile.WLU_ADMIN
        );
        assertThat(caseHistoryTab.getForRoles()).doesNotContain(
            AccessProfile.CITIZEN,
            AccessProfile.DEFENDANT,
            AccessProfile.DEFENDANT_SOLICITOR
        );
        assertThat(caseLinksTab.getForRoles()).containsAll(caseHistoryTab.getForRoles());
        assertThat(caseNotesTab.getForRoles()).containsAll(caseHistoryTab.getForRoles());
        assertThat(caseFlagsTab.getForRoles()).containsAll(caseHistoryTab.getForRoles());
    }
}
