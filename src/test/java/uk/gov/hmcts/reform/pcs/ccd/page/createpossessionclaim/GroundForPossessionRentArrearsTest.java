package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.MandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;

class GroundForPossessionRentArrearsTest extends BasePageTest {

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new GroundForPossessionRentArrears());
    }

    @Test
    void shouldAutoPopulateMandatoryGroundsWhenSeriousRentArrearsGround8IsSelected() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .hasOtherAdditionalGrounds(YES)
            .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundForPossessionRentArrears");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getData().getMandatoryGrounds())
            .containsExactly(MandatoryGround.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getDiscretionaryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateDiscretionaryGroundsWhenRentArrearsGround10IsSelected() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10))
            .hasOtherAdditionalGrounds(YES)
            .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundForPossessionRentArrears");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getData().getDiscretionaryGrounds())
            .containsExactly(DiscretionaryGround.RENT_ARREARS_GROUND10);
        assertThat(response.getData().getMandatoryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateDiscretionaryGroundsWhenPersistentDelayGround11IsSelected() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.PERSISTENT_DELAY_GROUND11))
            .hasOtherAdditionalGrounds(YES)
            .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundForPossessionRentArrears");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getData().getDiscretionaryGrounds())
            .containsExactly(DiscretionaryGround.PERSISTENT_DELAY_GROUND11);
        assertThat(response.getData().getMandatoryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateBothMandatoryAndDiscretionaryGroundsWhenMultipleGroundsAreSelected() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(
                RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
                RentArrearsGround.RENT_ARREARS_GROUND10,
                RentArrearsGround.PERSISTENT_DELAY_GROUND11
            ))
            .hasOtherAdditionalGrounds(YES)
            .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundForPossessionRentArrears");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getData().getMandatoryGrounds())
            .containsExactly(MandatoryGround.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getDiscretionaryGrounds())
            .containsExactlyInAnyOrder(
                DiscretionaryGround.RENT_ARREARS_GROUND10,
                DiscretionaryGround.PERSISTENT_DELAY_GROUND11
            );
    }

    @Test
    void shouldNotAutoPopulateWhenNoRentArrearsGroundsAreSelected() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of())
            .hasOtherAdditionalGrounds(YES)
            .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundForPossessionRentArrears");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getData().getMandatoryGrounds()).isEmpty();
        assertThat(response.getData().getDiscretionaryGrounds()).isEmpty();
    }

    @Test
    void shouldNotAutoPopulateWhenRentArrearsGroundsIsNull() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(null)
            .hasOtherAdditionalGrounds(YES)
            .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundForPossessionRentArrears");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getData().getMandatoryGrounds()).isNull();
        assertThat(response.getData().getDiscretionaryGrounds()).isNull();
    }

    @Test
    void shouldInitializeEmptyListsWhenMandatoryAndDiscretionaryGroundsAreNull() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .mandatoryGrounds(null)
            .discretionaryGrounds(null)
            .hasOtherAdditionalGrounds(YES)
            .build();
        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundForPossessionRentArrears");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response.getData().getMandatoryGrounds())
            .containsExactly(MandatoryGround.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getDiscretionaryGrounds()).isEmpty();
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
            .copyOfRentArrearsGrounds(new HashSet<>(previousGrounds))
            .build();

        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        caseDetails.setData(caseData);

        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "groundForPossessionRentArrears");

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        if (expectOverride) {
            int totalExpected = currentGrounds.size();
            int totalActual = response.getData().getMandatoryGrounds().size()
                + response.getData().getDiscretionaryGrounds().size();
            assertThat(totalActual).isEqualTo(totalExpected);
        } else {
            assertThat(response.getData().getMandatoryGrounds()).isNull();
            assertThat(response.getData().getDiscretionaryGrounds()).isNull();
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
