package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class RentArrearsOrBreachOfTenancyGroundTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsOrBreachOfTenancyGround());
    }

    @ParameterizedTest
    @MethodSource("midEventScenarios")
    void shouldSetDisplayFlagsInMidEventCallback(
        Set<RentArrearsOrBreachOfTenancy> rentAreasOrBreach,
        YesOrNo expectedShowBreachOfTenancyTextarea) {

        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsOrBreachOfTenancy(rentAreasOrBreach)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getShowBreachOfTenancyTextarea()).isEqualTo(expectedShowBreachOfTenancyTextarea);
    }

    @ParameterizedTest
    @MethodSource("provideRentDetailsPageScenarios")
    void shouldSetCorrectShowRentDetailsPageFlagForSecureFlexibleTenancy(
        Set<RentArrearsOrBreachOfTenancy> rentArrearsOrBreach,
        YesOrNo expectedShowRentDetailsPage) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsOrBreachOfTenancy(rentArrearsOrBreach)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        PCSCase updatedCaseData = response.getData();

        // Then
        assertThat(updatedCaseData.getShowRentSection()).isEqualTo(expectedShowRentDetailsPage);
    }

    private static Stream<Arguments> midEventScenarios() {
        return Stream.of(
            arguments(
                Set.of(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY),
                YesOrNo.YES
            ),
            arguments(
                Set.of(RentArrearsOrBreachOfTenancy.RENT_ARREARS),
                YesOrNo.NO
            ),
            arguments(
                Set.of(),
                YesOrNo.NO
            )
        );
    }

    private static Stream<Arguments> provideRentDetailsPageScenarios() {
        return Stream.of(
            // AC03 & AC04: Rent Arrears selected - Should show Rent Details
            arguments(Set.of(RentArrearsOrBreachOfTenancy.RENT_ARREARS), YesOrNo.YES),

            // AC05 & AC06: Breach of Tenancy only - Should NOT show Rent Details
            arguments(Set.of(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY), YesOrNo.NO),

            // Both selected - Should show Rent Details (rent arrears present)
            arguments(Set.of(RentArrearsOrBreachOfTenancy.RENT_ARREARS,
                            RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY), YesOrNo.YES),

            // Neither selected - Should NOT show Rent Details
            arguments(Set.of(), YesOrNo.NO)
        );
    }

}
