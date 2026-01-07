package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsAdditionalGrounds;


import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;

@ExtendWith(MockitoExtension.class)
class RentArrearsGroundsForPossessionTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsGroundsForPossession());
    }

    @Test
    void shouldAutoPopulateMandatoryGroundsWhenSeriousRentArrearsGround8IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .rentArrearsAdditionalGrounds(RentArrearsAdditionalGrounds.builder().build())
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getMandatoryGrounds())
            .containsExactly(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getDiscretionaryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateDiscretionaryGroundsWhenRentArrearsGround10IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10))
            .rentArrearsAdditionalGrounds(RentArrearsAdditionalGrounds.builder().build())
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getDiscretionaryGrounds())
            .containsExactly(RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10);
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getMandatoryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateDiscretionaryGroundsWhenPersistentDelayGround11IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.PERSISTENT_DELAY_GROUND11))
            .rentArrearsAdditionalGrounds(RentArrearsAdditionalGrounds.builder().build())
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getDiscretionaryGrounds())
            .containsExactly(RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11);
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getMandatoryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateBothMandatoryAndDiscretionaryGroundsWhenMultipleGroundsAreSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(
                RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
                RentArrearsGround.RENT_ARREARS_GROUND10,
                RentArrearsGround.PERSISTENT_DELAY_GROUND11
            ))
            .rentArrearsAdditionalGrounds(RentArrearsAdditionalGrounds.builder().build())
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getMandatoryGrounds())
            .containsExactly(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getDiscretionaryGrounds())
            .containsExactlyInAnyOrder(
                RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10,
                RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11
            );
    }

    @Test
    void shouldNotAutoPopulateWhenNoRentArrearsGroundsAreSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of())
            .rentArrearsAdditionalGrounds(RentArrearsAdditionalGrounds.builder().build())
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getMandatoryGrounds()).isEmpty();
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getDiscretionaryGrounds()).isEmpty();
    }

    @Test
    void shouldNotAutoPopulateWhenRentArrearsGroundsIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(null)
            .rentArrearsAdditionalGrounds(RentArrearsAdditionalGrounds.builder().build())
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getMandatoryGrounds()).isNull();
        assertThat(response.getData().getRentArrearsAdditionalGrounds().getDiscretionaryGrounds()).isNull();
    }

    @Test
    void shouldInitializeEmptyListsWhenMandatoryAndDiscretionaryGroundsAreNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .rentArrearsAdditionalGrounds(RentArrearsAdditionalGrounds.builder().build())

            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        RentArrearsAdditionalGrounds additionalGrounds =
            response.getData().getRentArrearsAdditionalGrounds();

        assertThat(additionalGrounds.getMandatoryGrounds())
            .containsExactly(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(additionalGrounds.getDiscretionaryGrounds()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideRentArrearsChangeScenarios")
    void shouldOverrideGroundsOnlyWhenSelectedRentArrearsGroundsChange(
        Set<RentArrearsGround> previousGrounds,
        Set<RentArrearsGround> currentGrounds,
        boolean expectOverride) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(currentGrounds)
            .rentArrearsAdditionalGrounds(RentArrearsAdditionalGrounds.builder().build())
            .copyOfRentArrearsGrounds(previousGrounds)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (expectOverride) {
            int totalExpected = currentGrounds.size();
            int totalActual = response.getData().getRentArrearsAdditionalGrounds().getMandatoryGrounds().size()
                + response.getData().getRentArrearsAdditionalGrounds().getDiscretionaryGrounds().size();
            assertThat(totalActual).isEqualTo(totalExpected);
        } else {
            assertThat(response.getData().getRentArrearsAdditionalGrounds().getMandatoryGrounds()).isNull();
            assertThat(response.getData().getRentArrearsAdditionalGrounds()
                           .getDiscretionaryGrounds()).isNull();
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
