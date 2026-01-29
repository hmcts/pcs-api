package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160;

class GroundsForPossessionWalesPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new GroundsForPossessionWalesPage());
    }

    @ParameterizedTest
    @MethodSource("groundsScenarios")
    void shouldValidateGroundsSelection(
            Set<DiscretionaryGroundWales> discretionaryGrounds,
            Set<EstateManagementGroundsWales> estateManagementGrounds,
            Set<MandatoryGroundWales> mandatoryGrounds,
            List<String> expectedErrors) {

        // Given
        PCSCase caseData = PCSCase.builder()
                .groundsForPossessionWales(GroundsForPossessionWales.builder()
                        .discretionaryGrounds(discretionaryGrounds)
                        .estateManagementGrounds(estateManagementGrounds)
                        .mandatoryGrounds(mandatoryGrounds)
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response =
                callMidEventHandler(caseData);

        // Then
        if (expectedErrors != null && !expectedErrors.isEmpty()) {
            assertThat(response.getErrors())
                    .containsExactlyInAnyOrderElementsOf(expectedErrors);
        } else {
            assertThat(response.getErrors()).isNullOrEmpty();
        }
    }

    private static Stream<Arguments> groundsScenarios() {
        return Stream.of(
                // Valid: Only discretionary (non-estate)
                arguments(
                        Set.of(DiscretionaryGroundWales.RENT_ARREARS_S157),
                        Set.of(),
                        Set.of(),
                        List.of()
                ),

                // Valid: Discretionary with estate parent + one sub-selection
                arguments(
                        Set.of(ESTATE_MANAGEMENT_GROUNDS_S160),
                        Set.of(EstateManagementGroundsWales.BUILDING_WORKS),
                        Set.of(),
                        List.of()
                ),

                // Valid: Multiple discretionary incl. estate + sub-selections
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_S157,
                                ESTATE_MANAGEMENT_GROUNDS_S160
                        ),
                        Set.of(
                                EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES,
                                EstateManagementGroundsWales.CHARITIES
                        ),
                        Set.of(),
                        List.of()
                ),

                // Valid: Only mandatory (discretionary empty)
                arguments(
                        Set.of(),
                        Set.of(),
                        Set.of(MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181),
                        List.of()
                ),

                // Valid: Null discretionary but mandatory present
                arguments(
                        null,
                        Set.of(),
                        Set.of(MandatoryGroundWales.FAILURE_TO_GIVE_UP_POSSESSION_S170),
                        List.of()
                ),

                // Invalid: No grounds at all
                arguments(
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        List.of("Please select at least one ground.")
                ),

                // Invalid: Null discretionary and empty mandatory
                arguments(
                        null,
                        Set.of(),
                        Set.of(),
                        List.of("Please select at least one ground.")
                ),

                // Invalid: Estate parent selected, no sub-selection
                arguments(
                        Set.of(ESTATE_MANAGEMENT_GROUNDS_S160),
                        Set.of(),
                        Set.of(),
                        List.of(
                                "Please select at least one ground in ‘Estate management grounds (section 160)’."
                        )
                ),

                // Invalid: Estate parent selected, null sub-selection
                arguments(
                        Set.of(ESTATE_MANAGEMENT_GROUNDS_S160),
                        null,
                        Set.of(),
                        List.of(
                                "Please select at least one ground in ‘Estate management grounds (section 160)’."
                        )
                ),

                // Invalid: Multiple discretionary incl. estate, no sub-selection
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_S157,
                                ESTATE_MANAGEMENT_GROUNDS_S160
                        ),
                        Set.of(),
                        Set.of(),
                        List.of(
                                "Please select at least one ground in ‘Estate management grounds (section 160)’."
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("routingScenarios")
    void shouldSetCorrectRoutingFlags(
            Set<DiscretionaryGroundWales> discretionaryGrounds,
            Set<EstateManagementGroundsWales> estateManagementGrounds,
            Set<MandatoryGroundWales> mandatoryGrounds,
            YesOrNo expectedShowASBQuestionsPage,
            YesOrNo expectedShowReasonsForGroundsPage) {

        // Given
        PCSCase caseData = PCSCase.builder()
                .groundsForPossessionWales(GroundsForPossessionWales.builder()
                        .discretionaryGrounds(discretionaryGrounds)
                        .estateManagementGrounds(estateManagementGrounds)
                        .mandatoryGrounds(mandatoryGrounds)
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        PCSCase updatedCaseData = response.getData();
        assertThat(updatedCaseData.getShowASBQuestionsPageWales()).isEqualTo(expectedShowASBQuestionsPage);
        assertThat(updatedCaseData.getShowReasonsForGroundsPageWales()).isEqualTo(expectedShowReasonsForGroundsPage);
    }

    private static Stream<Arguments> routingScenarios() {
        return Stream.of(
                // Only rent arrears - go to pre-action protocol page
                arguments(
                        Set.of(DiscretionaryGroundWales.RENT_ARREARS_S157),
                        Set.of(),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.NO
                ),
                // Only ASB - show ASB questions page
                arguments(
                        Set.of(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157),
                        Set.of(),
                        Set.of(),
                        YesOrNo.YES,
                        YesOrNo.NO
                ),
                // Rent arrears + ASB - show ASB questions page (ASB takes precedence)
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_S157,
                                DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157
                        ),
                        Set.of(),
                        Set.of(),
                        YesOrNo.YES,
                        YesOrNo.NO
                ),
                // Only other breach - show reasons for grounds page
                arguments(
                        Set.of(DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157),
                        Set.of(),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + other breach - show reasons for grounds page
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_S157,
                                DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157
                        ),
                        Set.of(),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // ASB + other breach - show reasons for grounds page
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157,
                                DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157
                        ),
                        Set.of(),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + ASB + other breach - show reasons for grounds page
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_S157,
                                DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157,
                                DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157
                        ),
                        Set.of(),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Only estate management grounds - show reasons for grounds page
                arguments(
                        Set.of(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160),
                        Set.of(EstateManagementGroundsWales.BUILDING_WORKS),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + estate management - show reasons for grounds page
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_S157,
                                DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160
                        ),
                        Set.of(EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // ASB + estate management - show reasons for grounds page
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157,
                                DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160
                        ),
                        Set.of(EstateManagementGroundsWales.CHARITIES),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + ASB + estate management - show reasons for grounds page
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_S157,
                                DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157,
                                DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160
                        ),
                        Set.of(EstateManagementGroundsWales.BUILDING_WORKS),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Only mandatory grounds - show reasons for grounds page
                arguments(
                        Set.of(),
                        Set.of(),
                        Set.of(MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + other option (mandatory) - show reasons for grounds page
                arguments(
                        Set.of(DiscretionaryGroundWales.RENT_ARREARS_S157),
                        Set.of(),
                        Set.of(MandatoryGroundWales.FAILURE_TO_GIVE_UP_POSSESSION_S170),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // ASB + mandatory grounds - show reasons for grounds page
                arguments(
                        Set.of(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157),
                        Set.of(),
                        Set.of(MandatoryGroundWales.LANDLORD_NOTICE_PERIODIC_S178),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + ASB + mandatory grounds - show reasons for grounds page
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_S157,
                                DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157
                        ),
                        Set.of(),
                        Set.of(MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Other breach + mandatory grounds - show reasons for grounds page
                arguments(
                        Set.of(DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157),
                        Set.of(),
                        Set.of(MandatoryGroundWales.FAIL_TO_GIVE_UP_BREAK_NOTICE_S191),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Estate management + mandatory grounds - show reasons for grounds page
                arguments(
                        Set.of(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160),
                        Set.of(EstateManagementGroundsWales.CHARITIES),
                        Set.of(MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199),
                        YesOrNo.NO,
                        YesOrNo.YES
                )
        );
    }
}
