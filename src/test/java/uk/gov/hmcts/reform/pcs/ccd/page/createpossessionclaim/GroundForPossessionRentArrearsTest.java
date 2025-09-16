package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.DiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.MandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;

class GroundForPossessionRentArrearsTest extends BasePageTest {


    @BeforeEach
    void setUp() {
        setPageUnderTest(new GroundForPossessionRentArrears());
    }

    @Test
    void shouldAutoPopulateMandatoryGroundsWhenSeriousRentArrearsGround8IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(List.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getMandatoryGrounds())
            .containsExactly(MandatoryGround.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getDiscretionaryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateDiscretionaryGroundsWhenRentArrearsGround10IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(List.of(RentArrearsGround.RENT_ARREARS_GROUND10))
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getDiscretionaryGrounds())
            .containsExactly(DiscretionaryGround.RENT_ARREARS_GROUND10);
        assertThat(response.getData().getMandatoryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateDiscretionaryGroundsWhenPersistentDelayGround11IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(List.of(RentArrearsGround.PERSISTENT_DELAY_GROUND11))
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getDiscretionaryGrounds())
            .containsExactly(DiscretionaryGround.PERSISTENT_DELAY_GROUND11);
        assertThat(response.getData().getMandatoryGrounds()).isEmpty();
    }

    @Test
    void shouldAutoPopulateBothMandatoryAndDiscretionaryGroundsWhenMultipleGroundsAreSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Arrays.asList(
                RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
                RentArrearsGround.RENT_ARREARS_GROUND10,
                RentArrearsGround.PERSISTENT_DELAY_GROUND11
            ))
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

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
    void shouldNotAddDuplicateGroundsWhenGroundsAlreadyExist() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(List.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .mandatoryGrounds(List.of(MandatoryGround.SERIOUS_RENT_ARREARS_GROUND8))
            .discretionaryGrounds(List.of(DiscretionaryGround.RENT_ARREARS_GROUND10))
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getMandatoryGrounds())
            .containsExactly(MandatoryGround.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getDiscretionaryGrounds())
            .containsExactly(DiscretionaryGround.RENT_ARREARS_GROUND10);
    }

    @Test
    void shouldNotAutoPopulateWhenNoRentArrearsGroundsAreSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Collections.emptyList())
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getMandatoryGrounds()).isEmpty();
        assertThat(response.getData().getDiscretionaryGrounds()).isEmpty();
    }

    @Test
    void shouldNotAutoPopulateWhenRentArrearsGroundsIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(null)
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getMandatoryGrounds()).isEmpty();
        assertThat(response.getData().getDiscretionaryGrounds()).isEmpty();
    }

    @Test
    void shouldInitializeEmptyListsWhenMandatoryAndDiscretionaryGroundsAreNull() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(List.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .mandatoryGrounds(null)
            .discretionaryGrounds(null)
            .hasOtherAdditionalGrounds(YES)
            .build();
        caseDetails.setData(caseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getMandatoryGrounds())
            .containsExactly(MandatoryGround.SERIOUS_RENT_ARREARS_GROUND8);
        assertThat(response.getData().getDiscretionaryGrounds()).isEmpty();
    }
}
