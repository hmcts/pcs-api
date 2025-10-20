package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EvictionRisksPosedPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new EvictionRisksPosedPage());
    }

    @Test
    void shouldRequireAtLeastOneSelection() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Select at least one option");
    }

    @Test
    void shouldRequireAtLeastOneSelectionWhenNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(null)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Select at least one option");
    }

    @ParameterizedTest
    @MethodSource("validSelectionScenarios")
    void shouldAllowValidSelections(Set<RiskCategory> selectedRisks, String description) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(selectedRisks)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementOrder().getEnforcementRiskCategories()).isEqualTo(selectedRisks);
    }

    @Test
    void shouldClearViolentDetailsWhenDeselected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.FIREARMS_POSSESSION))
                .enforcementViolentDetails("Previous violent text")
                .enforcementFirearmsDetails("Firearms text")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getEnforcementOrder().getEnforcementViolentDetails()).isNull();
        assertThat(response.getData().getEnforcementOrder().getEnforcementFirearmsDetails()).isEqualTo("Firearms text");
    }

    @Test
    void shouldClearFirearmsDetailsWhenDeselected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .enforcementViolentDetails("Violent text")
                .enforcementFirearmsDetails("Previous firearms text")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getEnforcementOrder().getEnforcementViolentDetails()).isEqualTo("Violent text");
        assertThat(response.getData().getEnforcementOrder().getEnforcementFirearmsDetails()).isNull();
    }

    @Test
    void shouldClearCriminalDetailsWhenDeselected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .enforcementViolentDetails("Violent text")
                .enforcementCriminalDetails("Previous criminal text")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getEnforcementOrder().getEnforcementViolentDetails()).isEqualTo("Violent text");
        assertThat(response.getData().getEnforcementOrder().getEnforcementCriminalDetails()).isNull();
    }

    @Test
    void shouldPreserveUnrelatedDetailsWhenOneCategoryDeselected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(
                    RiskCategory.FIREARMS_POSSESSION,
                    RiskCategory.CRIMINAL_OR_ANTISOCIAL
                ))
                .enforcementViolentDetails("Should be cleared")
                .enforcementFirearmsDetails("Should be preserved")
                .enforcementCriminalDetails("Should be preserved")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getEnforcementOrder().getEnforcementViolentDetails()).isNull();
        assertThat(response.getData().getEnforcementOrder().getEnforcementFirearmsDetails())
            .isEqualTo("Should be preserved");
        assertThat(response.getData().getEnforcementOrder().getEnforcementCriminalDetails())
            .isEqualTo("Should be preserved");
    }

    private static Stream<Arguments> validSelectionScenarios() {
        return Stream.of(
            arguments(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE), "Single violent selection"),
            arguments(Set.of(RiskCategory.FIREARMS_POSSESSION), "Single firearms selection"),
            arguments(Set.of(RiskCategory.CRIMINAL_OR_ANTISOCIAL), "Single criminal selection"),
            arguments(Set.of(RiskCategory.VERBAL_OR_WRITTEN_THREATS), "Single verbal threats selection"),
            arguments(Set.of(RiskCategory.PROTEST_GROUP_MEMBER), "Single protest group selection"),
            arguments(Set.of(RiskCategory.AGENCY_VISITS), "Single agency visits selection"),
            arguments(Set.of(RiskCategory.AGGRESSIVE_ANIMALS), "Single aggressive animals selection"),
            arguments(Set.of(
                RiskCategory.VIOLENT_OR_AGGRESSIVE,
                RiskCategory.FIREARMS_POSSESSION
            ), "Two detail categories"),
            arguments(Set.of(
                RiskCategory.VIOLENT_OR_AGGRESSIVE,
                RiskCategory.FIREARMS_POSSESSION,
                RiskCategory.CRIMINAL_OR_ANTISOCIAL
            ), "All three detail categories"),
            arguments(Set.of(
                RiskCategory.VERBAL_OR_WRITTEN_THREATS,
                RiskCategory.PROTEST_GROUP_MEMBER,
                RiskCategory.AGENCY_VISITS,
                RiskCategory.AGGRESSIVE_ANIMALS
            ), "All non-detail categories"),
            arguments(Set.of(
                RiskCategory.VIOLENT_OR_AGGRESSIVE,
                RiskCategory.VERBAL_OR_WRITTEN_THREATS,
                RiskCategory.PROTEST_GROUP_MEMBER
            ), "Mixed detail and non-detail categories")
        );
    }
}
