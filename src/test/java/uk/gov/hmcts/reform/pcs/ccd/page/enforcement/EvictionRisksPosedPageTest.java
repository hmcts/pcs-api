package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(selected)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Select at least one option");
    }

    @ParameterizedTest
    @MethodSource("validSelectionScenarios")
    void shouldAllowValidSelections(Set<RiskCategory> selectedRisks) {
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
                .riskDetails(EnforcementRiskDetails.builder()
                    .enforcementViolentDetails("Previous violent text")
                    .enforcementFirearmsDetails("Firearms text")
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getEnforcementOrder().getRiskDetails().getEnforcementViolentDetails()).isNull();
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementFirearmsDetails()).isEqualTo("Firearms text");
    }

    @Test
    void shouldClearFirearmsDetailsWhenDeselected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .riskDetails(EnforcementRiskDetails.builder()
                    .enforcementViolentDetails("Violent text")
                    .enforcementFirearmsDetails("Previous firearms text")
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementViolentDetails()).isEqualTo("Violent text");
        assertThat(response.getData().getEnforcementOrder().getRiskDetails().getEnforcementFirearmsDetails()).isNull();
    }

    @Test
    void shouldClearCriminalDetailsWhenDeselected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .riskDetails(EnforcementRiskDetails.builder()
                    .enforcementViolentDetails("Violent text")
                    .enforcementCriminalDetails("Previous criminal text")
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementViolentDetails()).isEqualTo("Violent text");
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementCriminalDetails()).isNull();
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
                .riskDetails(EnforcementRiskDetails.builder()
                    .enforcementViolentDetails("Should be cleared")
                    .enforcementFirearmsDetails("Should be preserved")
                    .enforcementCriminalDetails("Should be preserved")
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getEnforcementOrder().getRiskDetails().getEnforcementViolentDetails()).isNull();
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementFirearmsDetails())
            .isEqualTo("Should be preserved");
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementCriminalDetails())
            .isEqualTo("Should be preserved");
    }

    private static Stream<Set<RiskCategory>> validSelectionScenarios() {
        return Stream.of(
            Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE),
            Set.of(RiskCategory.FIREARMS_POSSESSION),
            Set.of(RiskCategory.CRIMINAL_OR_ANTISOCIAL),
            Set.of(RiskCategory.VERBAL_OR_WRITTEN_THREATS),
            Set.of(RiskCategory.PROTEST_GROUP_MEMBER),
            Set.of(RiskCategory.AGENCY_VISITS),
            Set.of(RiskCategory.AGGRESSIVE_ANIMALS),
            Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE, RiskCategory.FIREARMS_POSSESSION),
            Set.of(
                RiskCategory.VIOLENT_OR_AGGRESSIVE,
                RiskCategory.FIREARMS_POSSESSION,
                RiskCategory.CRIMINAL_OR_ANTISOCIAL
            ),
            Set.of(
                RiskCategory.VERBAL_OR_WRITTEN_THREATS,
                RiskCategory.PROTEST_GROUP_MEMBER,
                RiskCategory.AGENCY_VISITS,
                RiskCategory.AGGRESSIVE_ANIMALS
            ),
            Set.of(
                RiskCategory.VIOLENT_OR_AGGRESSIVE,
                RiskCategory.VERBAL_OR_WRITTEN_THREATS,
                RiskCategory.PROTEST_GROUP_MEMBER
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
