package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;

@ExtendWith(MockitoExtension.class)
class RentArrearsGroundsForPossessionTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundsForPossession());
    }

    @Test
    void shouldAutoPopulateMandatoryGroundsWhenSeriousRentArrearsGround8IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                    .grounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
                    .hasOtherAdditionalGrounds(YES)
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsMandatoryGrounds())
            .containsExactly(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getRentArrearsDiscretionaryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateDiscretionaryGroundsWhenRentArrearsGround10IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                    .hasOtherAdditionalGrounds(YES)
                    .grounds(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10))
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsDiscretionaryGrounds())
            .containsExactly(RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10);
        assertThat(response.getData().getRentArrearsMandatoryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateDiscretionaryGroundsWhenPersistentDelayGround11IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                    .hasOtherAdditionalGrounds(YES)
                    .grounds(Set.of(RentArrearsGround.PERSISTENT_DELAY_GROUND11))
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsDiscretionaryGrounds())
            .containsExactly(RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11);
        assertThat(response.getData().getRentArrearsMandatoryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateBothMandatoryAndDiscretionaryGroundsWhenMultipleGroundsAreSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                    .hasOtherAdditionalGrounds(YES)
                    .grounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
                                    RentArrearsGround.RENT_ARREARS_GROUND10,
                                    RentArrearsGround.PERSISTENT_DELAY_GROUND11))
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsMandatoryGrounds())
            .containsExactly(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getRentArrearsDiscretionaryGrounds())
            .containsExactlyInAnyOrder(
                RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10,
                RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11
            );
    }

    @Test
    void shouldNotAutoPopulateWhenNoRentArrearsGroundsAreSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                    .hasOtherAdditionalGrounds(YES)
                    .grounds(Set.of())
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsMandatoryGrounds()).isEmpty();
        assertThat(response.getData().getRentArrearsDiscretionaryGrounds()).isEmpty();
    }

    @Test
    void shouldNotAutoPopulateWhenRentArrearsGroundsIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                    .hasOtherAdditionalGrounds(YES)
                    .grounds(null)
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsMandatoryGrounds()).isNull();
        assertThat(response.getData().getRentArrearsDiscretionaryGrounds()).isNull();
    }

    @Test
    void shouldInitializeEmptyListsWhenMandatoryAndDiscretionaryGroundsAreNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                    .hasOtherAdditionalGrounds(YES)
                    .grounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
                    .build()
            )
            .rentArrearsMandatoryGrounds(null)
            .rentArrearsDiscretionaryGrounds(null)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsMandatoryGrounds())
            .containsExactly(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getRentArrearsDiscretionaryGrounds()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideRentArrearsChangeScenarios")
    void shouldOverrideGroundsOnlyWhenSelectedRentArrearsGroundsChange(
        Set<RentArrearsGround> previousGrounds,
        Set<RentArrearsGround> currentGrounds,
        boolean expectOverride) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                    .grounds(currentGrounds)
                    .copyOfGrounds(previousGrounds)
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (expectOverride) {
            int totalExpected = currentGrounds.size();
            int totalActual = response.getData().getRentArrearsMandatoryGrounds().size()
                + response.getData().getRentArrearsDiscretionaryGrounds().size();
            assertThat(totalActual).isEqualTo(totalExpected);
        } else {
            assertThat(response.getData().getRentArrearsMandatoryGrounds()).isNull();
            assertThat(response.getData().getRentArrearsDiscretionaryGrounds()).isNull();
        }
    }

    private static Stream<Arguments> provideRentArrearsChangeScenarios() {
        return Stream.of(
            Arguments.of(
                Set.of(),
                Set.of(RentArrearsGround.RENT_ARREARS_GROUND10, RentArrearsGround.PERSISTENT_DELAY_GROUND11),
                true
            ),
            Arguments.of(
                Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8),
                Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8),
                false
            ),
            Arguments.of(
                Set.of(RentArrearsGround.RENT_ARREARS_GROUND10),
                Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8, RentArrearsGround.PERSISTENT_DELAY_GROUND11),
                true
            )
        );
    }

}
