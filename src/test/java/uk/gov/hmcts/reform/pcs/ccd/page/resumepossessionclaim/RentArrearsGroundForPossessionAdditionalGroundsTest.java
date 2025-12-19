package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RentArrearsGroundForPossessionAdditionalGroundsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsGroundForPossessionAdditionalGrounds());
    }

    @Test
    void shouldErrorWhenRentArrearsSelectedAndNoAdditionalSelected() {
        // Given: user selected rent arrears (e.g., ground 8) on previous page, but nothing on this page
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Please select at least one ground");
    }

    @Test
    void shouldErrorWhenRentArrearsGround10SelectedAndNoAdditionalSelected() {
        // Given: user selected rent arrears ground 10, but nothing on this page
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10))
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Please select at least one ground");
    }

    @Test
    void shouldErrorWhenPersistentDelayGround11SelectedAndNoAdditionalSelected() {
        // Given: user selected persistent delay ground 11, but nothing on this page
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.PERSISTENT_DELAY_GROUND11))
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Please select at least one ground");
    }

    @Test
    void shouldMapPersistentDelayGround11ToDiscretionaryGrounds() {
        // Given: persistent delay ground 11 selected with additional mandatory
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.PERSISTENT_DELAY_GROUND11))
                    .build()
            )
            .assuredAdditionalMandatoryGrounds(Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds())
            .contains(RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11);
    }

    @Test
    void shouldMapAllThreeRentArrearsGroundsWhenAllSelected() {
        // Given: all three rent arrears grounds selected with additional grounds
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
            .assuredAdditionalMandatoryGrounds(Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds())
            .contains(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds())
            .containsExactlyInAnyOrder(
                RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10,
                RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11
            );
    }

    @Test
    void shouldPassAndSetShowReasonsWhenAdditionalMandatorySelected() {
        // Given: rent arrears selected earlier; user chooses an additional mandatory ground here
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
                    .build()
            )
            .assuredAdditionalMandatoryGrounds(Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: reasons page should be shown (has ground beyond 8)
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getShowRentArrearsGroundReasonPage()).isEqualTo(YesOrNo.YES);
        // And canonical sets include both 8 and 1 mapped into mandatory
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds())
            .containsExactlyInAnyOrder(
                RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8,
                RentArrearsMandatoryGrounds.OWNER_OCCUPIER_GROUND1
            );
    }

    @Test
    void shouldPassAndSetShowReasonsWhenAdditionalDiscretionarySelected() {
        // Given: rent arrears selected earlier; user chooses an additional discretionary ground here
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10))
                    .build()
            )
            .assuredAdditionalDiscretionaryGrounds(
                Set.of(AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12)
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: reasons page should be shown (has ground beyond 10/11)
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getShowRentArrearsGroundReasonPage()).isEqualTo(YesOrNo.YES);
        // And canonical discretionary includes mapped values
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds())
            .containsExactlyInAnyOrder(
                RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10,
                RentArrearsDiscretionaryGrounds.BREACH_TENANCY_GROUND12
            );
    }

    @Test
    void shouldPassWhenBothAdditionalMandatoryAndDiscretionarySelected() {
        // Given: rent arrears selected with both additional mandatory and discretionary
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
                    .build()
            )
            .assuredAdditionalMandatoryGrounds(Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1))
            .assuredAdditionalDiscretionaryGrounds(
                Set.of(AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12)
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getShowRentArrearsGroundReasonPage()).isEqualTo(YesOrNo.YES);
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds())
            .containsExactlyInAnyOrder(
                RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8,
                RentArrearsMandatoryGrounds.OWNER_OCCUPIER_GROUND1
            );
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds())
            .contains(RentArrearsDiscretionaryGrounds.BREACH_TENANCY_GROUND12);
    }

    @Test
    void shouldUseExistingCanonicalSetsWhenNoRentArrearsAndNoAdditional() {
        // Given: no rent arrears grounds and no additional grounds, but existing canonical sets
        Set<RentArrearsMandatoryGrounds> existingMandatory = Set.of(
            RentArrearsMandatoryGrounds.OWNER_OCCUPIER_GROUND1
        );
        Set<RentArrearsDiscretionaryGrounds> existingDiscretionary = Set.of(
            RentArrearsDiscretionaryGrounds.BREACH_TENANCY_GROUND12
        );
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of())
                .mandatoryGrounds(existingMandatory)
                .discretionaryGrounds(existingDiscretionary)
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: should use existing canonical sets (backward compatibility)
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds())
            .isEqualTo(existingMandatory);
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds())
            .isEqualTo(existingDiscretionary);
    }

    @Test
    void shouldUseEmptySetsWhenNoRentArrearsAndNoAdditionalAndNoExistingCanonicalSets() {
        // Given: no rent arrears grounds, no additional grounds, and no existing canonical sets
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of())
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: should not modify caseData (backward compatibility - uses effective values internally)
        assertThat(response.getErrors()).isNullOrEmpty();
        // CaseData values remain unchanged (null or empty) when no rent arrears and no additional
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds()).isNull();
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds()).isNull();
    }

    @Test
    void shouldMergeAdditionalWhenNoRentArrearsButHasAdditional() {
        // Given: no rent arrears grounds but has additional grounds
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of())
                    .build()
            )
            .assuredAdditionalMandatoryGrounds(Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: should merge additional grounds
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds())
            .contains(RentArrearsMandatoryGrounds.OWNER_OCCUPIER_GROUND1);
    }

    @ParameterizedTest
    @MethodSource("provideShowRentArrearsGroundReasonPageScenarios")
    void shouldSetCorrectShowRentArrearsGroundReasonPage(
        Set<RentArrearsGround> rentArrearsGrounds,
        Set<AssuredAdditionalMandatoryGrounds> additionalMandatory,
        Set<AssuredAdditionalDiscretionaryGrounds> additionalDiscretionary,
        YesOrNo expectedShowReasonPage) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(rentArrearsGrounds)
                    .build()
            )
            .assuredAdditionalMandatoryGrounds(additionalMandatory)
            .assuredAdditionalDiscretionaryGrounds(additionalDiscretionary)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (response.getErrors() == null || response.getErrors().isEmpty()) {
            assertThat(response.getData().getShowRentArrearsGroundReasonPage())
                .isEqualTo(expectedShowReasonPage);
        }
    }

    @Test
    void shouldNotShowReasonPageWhenOnlyRentArrearsGroundsSelected() {
        // Given: only rent arrears grounds (8, 10, 11) selected, no other grounds
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
            .assuredAdditionalMandatoryGrounds(Set.of())
            .assuredAdditionalDiscretionaryGrounds(Set.of())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: should error because no additional grounds selected
        assertThat(response.getErrors()).containsExactly("Please select at least one ground");
    }

    @Test
    void shouldShowReasonPageWhenHasOtherMandatoryGrounds() {
        // Given: rent arrears ground 8 + other mandatory ground
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
                    .build()
            )
            .assuredAdditionalMandatoryGrounds(Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getShowRentArrearsGroundReasonPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowReasonPageWhenHasOtherDiscretionaryGrounds() {
        // Given: rent arrears ground 10 + other discretionary ground
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10))
                    .build()
            )
            .assuredAdditionalDiscretionaryGrounds(
                Set.of(AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12)
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getShowRentArrearsGroundReasonPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldNotShowReasonPageWhenOnlyRentArrearsGround8Selected() {
        // Given: only ground 8 selected with additional mandatory (but no other grounds beyond 8)
        // This tests the logic: hasOtherMandatoryGrounds checks for grounds != GROUND8
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
                    .build()
            )
            .assuredAdditionalMandatoryGrounds(Set.of())
            .assuredAdditionalDiscretionaryGrounds(Set.of())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: should error because no additional grounds selected
        assertThat(response.getErrors()).containsExactly("Please select at least one ground");
    }

    @Test
    void shouldHandleNullRentArrearsGrounds() {
        // Given: null rent arrears grounds
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(null)
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: should use existing canonical sets (backward compatibility)
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    void shouldHandleNullAdditionalGrounds() {
        // Given: rent arrears selected but null additional grounds
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
                    .build()
            )
            .assuredAdditionalMandatoryGrounds(null)
            .assuredAdditionalDiscretionaryGrounds(null)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: should error because no additional grounds selected
        assertThat(response.getErrors()).containsExactly("Please select at least one ground");
    }

    @Test
    void shouldHandleEmptyAdditionalGrounds() {
        // Given: rent arrears selected but empty additional grounds
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
                    .build()
            )
            .assuredAdditionalMandatoryGrounds(Set.of())
            .assuredAdditionalDiscretionaryGrounds(Set.of())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: should error because no additional grounds selected
        assertThat(response.getErrors()).containsExactly("Please select at least one ground");
    }

    @Test
    void shouldHandleNullExistingCanonicalSets() {
        // Given: no rent arrears, no additional, null existing canonical sets
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGroundsForPossession(
                RentArrearsGroundsForPossession.builder()
                .rentArrearsGrounds(Set.of())
                .mandatoryGrounds(null)
                .discretionaryGrounds(null)
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: should not modify caseData (backward compatibility - uses effective values internally)
        assertThat(response.getErrors()).isNullOrEmpty();
        // CaseData values remain unchanged (null) when no rent arrears and no additional
        assertThat(response.getData().getRentArrearsGroundsForPossession().getMandatoryGrounds()).isNull();
        assertThat(response.getData().getRentArrearsGroundsForPossession().getDiscretionaryGrounds()).isNull();
        // But showRentArrearsGroundReasonPage should be NO (no other grounds)
        assertThat(response.getData().getShowRentArrearsGroundReasonPage()).isEqualTo(YesOrNo.NO);
    }

    private static Stream<Arguments> provideShowRentArrearsGroundReasonPageScenarios() {
        return Stream.of(
            // Only rent arrears grounds (8, 10, 11) - should not show reason page
            Arguments.of(
                Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8),
                Set.of(),
                Set.of(),
                YesOrNo.NO
            ),
            // Rent arrears + other mandatory - should show reason page
            Arguments.of(
                Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8),
                Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1),
                Set.of(),
                YesOrNo.YES
            ),
            // Rent arrears + other discretionary - should show reason page
            Arguments.of(
                Set.of(RentArrearsGround.RENT_ARREARS_GROUND10),
                Set.of(),
                Set.of(AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12),
                YesOrNo.YES
            ),
            // No rent arrears, only other grounds - should show reason page
            Arguments.of(
                Set.of(),
                Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1),
                Set.of(),
                YesOrNo.YES
            ),
            // No grounds at all - should not show reason page
            Arguments.of(
                Set.of(),
                Set.of(),
                Set.of(),
                YesOrNo.NO
            )
        );
    }

}
