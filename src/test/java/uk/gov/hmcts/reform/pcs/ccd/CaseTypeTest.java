package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.api.PropertyUtils;
import uk.gov.hmcts.ccd.sdk.api.Search;
import uk.gov.hmcts.ccd.sdk.api.SearchCases;
import uk.gov.hmcts.ccd.sdk.api.Tab;
import uk.gov.hmcts.ccd.sdk.api.TabField;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CaseTypeTest {

    @InjectMocks
    private CaseType caseType;

    @Mock
    private ConfigBuilderImpl<PCSCase, State, UserRole> builder;

    @Mock
    private PropertyUtils utils;

    @Test
    void shouldGetCaseType() {
        // When
        String caseType = CaseType.getCaseType();

        // Then
        assertThat(caseType).isEqualTo("PCS");
    }

    @Test
    void shouldGetJurisdictionId() {
        // When
        String jurisdictionId = CaseType.getJurisdictionId();

        // Then
        assertThat(jurisdictionId).isEqualTo("PCS");
    }

    @Test
    void shouldGetCaseTypeName() {
        // When
        String caseTypeName = CaseType.getCaseTypeName();

        // Then
        assertThat(caseTypeName).isEqualTo("Possession");
    }

    @Test
    void shouldConfigureCaseTypeTabs() {
        // Given
        final Tab.TabBuilder<PCSCase, UserRole> nextStepsTabBuilder = Tab.TabBuilder.builder(PCSCase.class, utils);
        final Tab.TabBuilder<PCSCase, UserRole> summaryTabBuilder = Tab.TabBuilder.builder(PCSCase.class, utils);
        final Tab.TabBuilder<PCSCase, UserRole> caseHistoryTabBuilder = Tab.TabBuilder.builder(PCSCase.class, utils);
        final Tab.TabBuilder<PCSCase, UserRole> hiddenTabBuilder = Tab.TabBuilder.builder(PCSCase.class, utils);
        final Tab.TabBuilder<PCSCase, UserRole> serviceRequestTabBuilder = Tab.TabBuilder.builder(PCSCase.class, utils);
        final Tab.TabBuilder<PCSCase, UserRole> caseLinksTabBuilder = Tab.TabBuilder.builder(PCSCase.class, utils);
        final Tab.TabBuilder<PCSCase, UserRole> casePartiesTabBuilder = Tab.TabBuilder.builder(PCSCase.class, utils);
        final Search.SearchBuilder<PCSCase, UserRole> searchBuilder =
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
        when(builder.tab("caseLinks", "Linked cases")).thenReturn(caseLinksTabBuilder);
        when(builder.tab("caseParties", "Case Parties")).thenReturn(casePartiesTabBuilder);

        // When
        caseType.configure(builder);
        final Tab<PCSCase, UserRole> nextStepsTab = nextStepsTabBuilder.build();
        final Tab<PCSCase, UserRole> summaryTab = summaryTabBuilder.build();
        final Tab<PCSCase, UserRole> caseHistoryTab = caseHistoryTabBuilder.build();
        final Tab<PCSCase, UserRole> hiddenTab = hiddenTabBuilder.build();
        final Tab<PCSCase, UserRole> serviceRequestTab = serviceRequestTabBuilder.build();
        final Tab<PCSCase, UserRole> caseLinksTab = caseLinksTabBuilder.build();
        final Tab<PCSCase, UserRole> casePartiesTab = casePartiesTabBuilder.build();

        // Then
        assertThat(nextStepsTab.getFields()).extracting(TabField::getId).contains("nextStepsMarkdown");
        assertThat(summaryTab.getFields()).extracting(TabField::getId).contains("confirmEvictionSummaryMarkup");
        assertThat(caseHistoryTab.getFields()).extracting(TabField::getId).contains("caseHistory");
        assertThat(hiddenTab.getFields().size()).isEqualTo(1);
        assertThat(serviceRequestTab.getFields()).extracting(TabField::getId).contains("waysToPay");
        assertThat(caseLinksTab.getFields()).extracting(TabField::getShowCondition)
            .contains("LinkedCasesComponentLauncher!=\"\"");
        assertThat(casePartiesTab.getFields()).extracting(TabField::getId).contains("casePartiesTab_ClaimantDetails");
    }
}
