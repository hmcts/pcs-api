package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EvictionRisksPosedPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new EvictionRisksPosedPage());
    }

    @ParameterizedTest
    @MethodSource("invalidSelectionScenarios")
    void shouldRequireAtLeastOneSelectionInvalid(Set<RiskCategory> selected) {
        // Given
        PCSCase caseData = createCaseData(selected, null);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Select at least one option");
    }

    @ParameterizedTest
    @MethodSource("validSelectionScenarios")
    void shouldAllowValidSelections(Set<RiskCategory> selectedRisks) {
        // Given
        PCSCase caseData = createCaseData(selectedRisks, null);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder().getEnforcementRiskCategories()).isEqualTo(selectedRisks);
    }

    @ParameterizedTest
    @MethodSource("dataPreservationScenarios")
    void shouldPreserveDetailsWhenCategoriesDeselected(Set<RiskCategory> selectedCategories,
                                                       String violentDetails,
                                                       String firearmsDetails,
                                                       String criminalDetails,
                                                       String expectedViolentDetails,
                                                       String expectedFirearmsDetails,
                                                       String expectedCriminalDetails) {
        // Given
        PCSCase caseData = createCaseData(selectedCategories, 
            EnforcementRiskDetails.builder()
                .enforcementViolentDetails(violentDetails)
                .enforcementFirearmsDetails(firearmsDetails)
                .enforcementCriminalDetails(criminalDetails)
                .build());

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getEnforcementOrder().getRiskDetails().getEnforcementViolentDetails())
            .isEqualTo(expectedViolentDetails);
        assertThat(response.getData().getEnforcementOrder().getRiskDetails().getEnforcementFirearmsDetails())
            .isEqualTo(expectedFirearmsDetails);
        assertThat(response.getData().getEnforcementOrder().getRiskDetails().getEnforcementCriminalDetails())
            .isEqualTo(expectedCriminalDetails);
    }

    @Test
    void shouldPreserveAllDetailsWhenAllCategoriesDeselected() {
        // Given - select no categories but have all details
        PCSCase caseData = createCaseData(Set.of(), 
            EnforcementRiskDetails.builder()
                .enforcementViolentDetails("All violent details")
                .enforcementFirearmsDetails("All firearms details")
                .enforcementCriminalDetails("All criminal details")
                .enforcementVerbalOrWrittenThreatsDetails("All verbal details")
                .enforcementProtestGroupMemberDetails("All protestor details")
                .build());

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then - should get validation error but data should be preserved
        assertThat(response.getErrors()).containsExactly("Select at least one option");
        assertThat(response.getData().getEnforcementOrder().getRiskDetails().getEnforcementViolentDetails())
            .isEqualTo("All violent details");
        assertThat(response.getData().getEnforcementOrder().getRiskDetails().getEnforcementFirearmsDetails())
            .isEqualTo("All firearms details");
        assertThat(response.getData().getEnforcementOrder().getRiskDetails().getEnforcementCriminalDetails())
            .isEqualTo("All criminal details");
        assertThat(response.getData().getEnforcementOrder().getRiskDetails()
                .getEnforcementVerbalOrWrittenThreatsDetails())
                .isEqualTo("All verbal details");
        assertThat(response.getData().getEnforcementOrder().getRiskDetails().getEnforcementProtestGroupMemberDetails())
                .isEqualTo("All protestor details");
    }

    @Test
    void shouldHandleNullRiskDetails() {
        // Given
        PCSCase caseData = createCaseData(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE), null);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder().getRiskDetails()).isNotNull();
    }

    @Test
    void shouldHandleAllRiskCategoriesSelected() {
        // Given
        Set<RiskCategory> allCategories = Set.of(
            RiskCategory.VIOLENT_OR_AGGRESSIVE,
            RiskCategory.FIREARMS_POSSESSION,
            RiskCategory.CRIMINAL_OR_ANTISOCIAL,
            RiskCategory.VERBAL_OR_WRITTEN_THREATS,
            RiskCategory.PROTEST_GROUP_MEMBER,
            RiskCategory.AGENCY_VISITS,
            RiskCategory.AGGRESSIVE_ANIMALS
        );
        PCSCase caseData = createCaseData(allCategories, null);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder().getEnforcementRiskCategories())
            .isEqualTo(allCategories);
    }

    // Helper method to reduce duplication
    private PCSCase createCaseData(Set<RiskCategory> selectedCategories, EnforcementRiskDetails riskDetails) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(selectedCategories)
                .riskDetails(riskDetails)
                .build())
            .build();
    }

    private static Stream<Arguments> dataPreservationScenarios() {
        return Stream.of(
            // Test preserving violent details when deselected
            Arguments.of(
                Set.of(RiskCategory.FIREARMS_POSSESSION),
                "Previous violent text",
                "Firearms text",
                null,
                "Previous violent text",  // Should be preserved
                "Firearms text",          // Should be preserved
                null                      // Should remain null
            ),
            // Test preserving firearms details when deselected
            Arguments.of(
                Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE),
                "Violent text",
                "Previous firearms text",
                null,
                "Violent text",           // Should be preserved
                "Previous firearms text", // Should be preserved
                null                      // Should remain null
            ),
            // Test preserving criminal details when deselected
            Arguments.of(
                Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE),
                "Violent text",
                null,
                "Previous criminal text",
                "Violent text",           // Should be preserved
                null,                    // Should remain null
                "Previous criminal text"  // Should be preserved
            ),
            // Test preserving all details when multiple categories deselected
            Arguments.of(
                Set.of(RiskCategory.FIREARMS_POSSESSION, RiskCategory.CRIMINAL_OR_ANTISOCIAL),
                "Should be preserved",
                "Should be preserved",
                "Should be preserved",
                "Should be preserved",
                "Should be preserved",
                "Should be preserved"
            )
        );
    }

    private static Stream<Set<RiskCategory>> validSelectionScenarios() {
        return Stream.of(
            // Single category selections
            Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE),
            Set.of(RiskCategory.FIREARMS_POSSESSION),
            Set.of(RiskCategory.CRIMINAL_OR_ANTISOCIAL),
            Set.of(RiskCategory.VERBAL_OR_WRITTEN_THREATS),
            Set.of(RiskCategory.PROTEST_GROUP_MEMBER),
            Set.of(RiskCategory.AGENCY_VISITS),
            Set.of(RiskCategory.AGGRESSIVE_ANIMALS),
            // Multiple category combinations
            Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE, RiskCategory.FIREARMS_POSSESSION),
            Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE, RiskCategory.FIREARMS_POSSESSION, 
                RiskCategory.CRIMINAL_OR_ANTISOCIAL),
            Set.of(RiskCategory.VERBAL_OR_WRITTEN_THREATS, RiskCategory.PROTEST_GROUP_MEMBER, 
                RiskCategory.AGENCY_VISITS, RiskCategory.AGGRESSIVE_ANIMALS),
            Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE, RiskCategory.VERBAL_OR_WRITTEN_THREATS, 
                RiskCategory.PROTEST_GROUP_MEMBER),
            // All categories
            Set.of(
                RiskCategory.VIOLENT_OR_AGGRESSIVE,
                RiskCategory.FIREARMS_POSSESSION,
                RiskCategory.CRIMINAL_OR_ANTISOCIAL,
                RiskCategory.VERBAL_OR_WRITTEN_THREATS,
                RiskCategory.PROTEST_GROUP_MEMBER,
                RiskCategory.AGENCY_VISITS,
                RiskCategory.AGGRESSIVE_ANIMALS
            )
        );
    }

    private static Stream<Set<RiskCategory>> invalidSelectionScenarios() {
        return Stream.of(
            Set.of(),
            null
        );
    }
}