package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_SECTION_160;

public class GroundsForPossessionWalesTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new GroundsForPossessionWales());
    }

    @ParameterizedTest
    @MethodSource("groundsScenarios")
    void shouldValidateGroundsSelection(
            Set<DiscretionaryGroundWales> discretionaryGrounds,
            Set<EstateManagementGroundWales> estateManagementGrounds,
            Set<MandatoryGroundWales> mandatoryGrounds,
            List<String> expectedErrors) {

        // Given
        PCSCase caseData = PCSCase.builder()
                .discretionaryGroundsWales(discretionaryGrounds)
                .estateManagementGroundsWales(estateManagementGrounds)
                .mandatoryGroundsWales(mandatoryGrounds)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (expectedErrors != null && !expectedErrors.isEmpty()) {
            assertThat(response.getErrors()).containsExactlyInAnyOrderElementsOf(expectedErrors);
        } else {
            assertThat(response.getErrors()).isNullOrEmpty();
        }
    }

    private static Stream<Arguments> groundsScenarios() {
        return Stream.of(
                // Valid: Only discretionary ground (not estate management)
                arguments(
                        Set.of(DiscretionaryGroundWales.RENT_ARREARS_SECTION_157),
                        Set.of(),
                        Set.of(),
                        List.of()
                ),
                // Valid: Discretionary with estate management parent and sub-selection
                arguments(
                        Set.of(ESTATE_MANAGEMENT_GROUNDS_SECTION_160),
                        Set.of(EstateManagementGroundWales.BUILDING_WORKS_A),
                        Set.of(),
                        List.of()
                ),
                // Valid: Multiple discretionary grounds including estate management with sub-selection
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_SECTION_157,
                                ESTATE_MANAGEMENT_GROUNDS_SECTION_160
                        ),
                        Set.of(
                                EstateManagementGroundWales.REDEVELOPMENT_B,
                                EstateManagementGroundWales.CHARITIES_C
                        ),
                        Set.of(),
                        List.of()
                ),
                // Valid: Only mandatory ground
                arguments(
                        Set.of(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_SECTION_157),
                        Set.of(),
                        Set.of(MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181),
                        List.of()
                ),
                // Valid: Mixed discretionary and mandatory
                arguments(
                        Set.of(DiscretionaryGroundWales.OTHER_BREACH_SECTION_157),
                        Set.of(),
                        Set.of(
                                MandatoryGroundWales.FAIL_TO_GIVE_UP_S170,
                                MandatoryGroundWales.LANDLORD_NOTICE_PERIODIC_S178
                        ),
                        List.of()
                ),
                // Invalid: No grounds selected
                arguments(
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        List.of("Select at least one discretionary ground.")
                ),
                // Invalid: Null discretionary grounds
                arguments(
                        null,
                        Set.of(),
                        Set.of(),
                        List.of("Select at least one discretionary ground.")
                ),
                // Invalid: Estate management parent selected but no sub-selection
                arguments(
                        Set.of(ESTATE_MANAGEMENT_GROUNDS_SECTION_160),
                        Set.of(),
                        Set.of(),
                        List.of("Select at least one estate management ground when "
                                + "'Estate management grounds (section 160)' is selected.")
                ),
                // Invalid: Estate management parent with null sub-selection
                arguments(
                        Set.of(ESTATE_MANAGEMENT_GROUNDS_SECTION_160),
                        null,
                        Set.of(),
                        List.of("Select at least one estate management ground when "
                                + "'Estate management grounds (section 160)' is selected.")
                ),
                // Invalid: Multiple discretionary including estate management but no sub-selection
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_SECTION_157,
                                ESTATE_MANAGEMENT_GROUNDS_SECTION_160
                        ),
                        Set.of(),
                        Set.of(),
                        List.of("Select at least one estate management ground when "
                                + "'Estate management grounds (section 160)' is selected.")
                )
        );
    }
}
