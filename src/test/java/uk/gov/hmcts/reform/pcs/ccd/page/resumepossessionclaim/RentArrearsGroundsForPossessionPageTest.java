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
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;

@ExtendWith(MockitoExtension.class)
class RentArrearsGroundsForPossessionPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsGroundsForPossessionPage());
    }

    @Test
    void shouldAutoPopulateMandatoryGroundsWhenSeriousRentArrearsGround8IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
                    .build()
            )
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds())
            .containsExactly(AssuredMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateDiscretionaryGroundsWhenRentArrearsGround10IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10))
                    .build()
            )
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds())
            .containsExactly(AssuredDiscretionaryGrounds.RENT_ARREARS_GROUND10);
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateDiscretionaryGroundsWhenPersistentDelayGround11IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.PERSISTENT_DELAY_GROUND11))
                    .build()
            )
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds())
            .containsExactly(AssuredDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11);
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateBothMandatoryAndDiscretionaryGroundsWhenMultipleGroundsAreSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(
                    RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
                    RentArrearsGround.RENT_ARREARS_GROUND10,
                    RentArrearsGround.PERSISTENT_DELAY_GROUND11
                ))
                    .build()
            )
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds())
            .containsExactly(AssuredMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds())
            .containsExactlyInAnyOrder(
                AssuredDiscretionaryGrounds.RENT_ARREARS_GROUND10,
                AssuredDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11
            );
    }

    @Test
    void shouldNotAutoPopulateWhenNoRentArrearsGroundsAreSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of())
                    .build()
            )
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds()).isEmpty();
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds()).isEmpty();
    }

    @Test
    void shouldNotAutoPopulateWhenRentArrearsGroundsIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(null)
                    .build()
            )
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds()).isNull();
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds()).isNull();
    }

    @Test
    void shouldInitializeEmptyListsWhenMandatoryAndDiscretionaryGroundsAreNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .mandatoryGrounds(null)
            .discretionaryGrounds(null)
                    .build()
            )
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds())
            .containsExactly(AssuredMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds()).isEmpty();
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
                .rentArrearsGrounds(currentGrounds)
                .copyOfRentArrearsGrounds(previousGrounds)
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (expectOverride) {
            int totalExpected = currentGrounds.size();
            int totalActual = response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds().size()
                + response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds().size();
            assertThat(totalActual).isEqualTo(totalExpected);
        } else {
            assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds()).isNull();
            assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds()).isNull();
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
