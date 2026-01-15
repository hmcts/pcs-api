package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
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
    void shouldSetDisplayFlagsForSuspensionDemotionAndCombinedPages(
        Set<AlternativesToPossession> alternativesToPossessionOptions,
        YesOrNo expectedSuspensionPageFlag,
        YesOrNo expectedDemotionPageFlag,
        YesOrNo expectedCombinedPageFlag) {

        // Given
        PCSCase caseData = PCSCase.builder()
            .alternativesToPossession(alternativesToPossessionOptions)
            .suspensionOfRightToBuy(SuspensionOfRightToBuy.builder().build())
            .demotionOfTenancy(DemotionOfTenancy.builder().build())
            .suspensionOfRightToBuyDemotionOfTenancy(SuspensionOfRightToBuyDemotionOfTenancy.builder().build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getSuspensionOfRightToBuy()
                       .getShowHousingActsPage()).isEqualTo(expectedSuspensionPageFlag);
        assertThat(response.getData().getDemotionOfTenancy()
                       .getShowHousingActsPage()).isEqualTo(expectedDemotionPageFlag);
        assertThat(response.getData().getSuspensionOfRightToBuyDemotionOfTenancy()
                       .getSuspensionToBuyDemotionOfTenancyPages()).isEqualTo(expectedCombinedPageFlag);
    }

    private static Stream<Arguments> midEventScenarios() {
        return Stream.of(
            // Only suspension selected
            arguments(
                Set.of(AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY),
                YesOrNo.YES,
                YesOrNo.NO,
                YesOrNo.NO
            ),
            // Only demotion selected
            arguments(
                Set.of(AlternativesToPossession.DEMOTION_OF_TENANCY),
                YesOrNo.NO,
                YesOrNo.YES,
                YesOrNo.NO
            ),
            // Both suspension and demotion selected
            arguments(
                Set.of(AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY,
                       AlternativesToPossession.DEMOTION_OF_TENANCY),
                YesOrNo.NO,
                YesOrNo.NO,
                YesOrNo.YES
            ),
            // Neither selected
            arguments(
                Set.of(),
                YesOrNo.NO,
                YesOrNo.NO,
                YesOrNo.NO
            )
        );
    }

}
