package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AlternativesToPossessionOptionsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new AlternativesToPossessionOptions());
    }

    @ParameterizedTest
    @MethodSource("midEventScenarios")
    void shouldSetDisplayFlagForSuspensionOfRightToBuyHousingActsPage(
        Set<AlternativesToPossession> alternativesToPossessionOptions,
        YesOrNo expectedShowCondition) {

        // Given
        PCSCase caseData = PCSCase.builder()
            .alternativesToPossession(alternativesToPossessionOptions)
            .suspensionOfRightToBuy(SuspensionOfRightToBuy.builder().build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getSuspensionOfRightToBuy().getShowSuspensionOfRightToBuyHousingActsPage())
            .isEqualTo(expectedShowCondition);
    }

    private static Stream<Arguments> midEventScenarios() {
        return Stream.of(
            arguments(
                Set.of(AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY),
                YesOrNo.YES
            ),
            arguments(
                Set.of(AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY,
                       AlternativesToPossession.DEMOTION_OF_TENANCY),
                YesOrNo.NO
            ),
            arguments(
                Set.of(AlternativesToPossession.DEMOTION_OF_TENANCY),
                YesOrNo.NO
            )
        );
    }

}
