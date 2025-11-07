package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import java.util.List;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.wales.WalesRentDetailsRoutingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_SECTION_160;

public class GroundsForPossessionWalesTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new GroundsForPossessionWales(
            new WalesRentDetailsRoutingService(List.of())
        ));
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
                .discretionaryGroundsWales(discretionaryGrounds)
                .estateManagementGroundsWales(estateManagementGrounds)
                .mandatoryGroundsWales(mandatoryGrounds)
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
                        Set.of(DiscretionaryGroundWales.RENT_ARREARS_SECTION_157),
                        Set.of(),
                        Set.of(),
                        List.of()
                ),

                // Valid: Discretionary with estate parent + one sub-selection
                arguments(
                        Set.of(ESTATE_MANAGEMENT_GROUNDS_SECTION_160),
                        Set.of(EstateManagementGroundsWales.BUILDING_WORKS),
                        Set.of(),
                        List.of()
                ),

                // Valid: Multiple discretionary incl. estate + sub-selections
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_SECTION_157,
                                ESTATE_MANAGEMENT_GROUNDS_SECTION_160
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
                        Set.of(MandatoryGroundWales.FAIL_TO_GIVE_UP_S170),
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
                        Set.of(ESTATE_MANAGEMENT_GROUNDS_SECTION_160),
                        Set.of(),
                        Set.of(),
                        List.of(
                                "Please select at least one ground in 'Estate management grounds (section 160)'."
                        )
                ),

                // Invalid: Estate parent selected, null sub-selection
                arguments(
                        Set.of(ESTATE_MANAGEMENT_GROUNDS_SECTION_160),
                        null,
                        Set.of(),
                        List.of(
                                "Please select at least one ground in 'Estate management grounds (section 160)'."
                        )
                ),

                // Invalid: Multiple discretionary incl. estate, no sub-selection
                arguments(
                        Set.of(
                                DiscretionaryGroundWales.RENT_ARREARS_SECTION_157,
                                ESTATE_MANAGEMENT_GROUNDS_SECTION_160
                        ),
                        Set.of(),
                        Set.of(),
                        List.of(
                                "Please select at least one ground in 'Estate management grounds (section 160)'."
                        )
                )
        );
    }
}
