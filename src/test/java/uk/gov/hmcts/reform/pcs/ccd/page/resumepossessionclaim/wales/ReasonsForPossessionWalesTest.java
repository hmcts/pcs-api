package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ReasonsForPossessionWalesTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ReasonsForPossessionWales());
    }

    @ParameterizedTest
    @MethodSource("asbRoutingScenarios")
    void shouldSetShowASBQuestionsPageWalesBasedOnGroundsSelection(
        Set<DiscretionaryGroundWales> discretionaryGrounds,
        Set<SecureContractDiscretionaryGroundsWales> secureDiscretionaryGrounds,
        YesOrNo expectedShowASBQuestionsPage) {

        PCSCase caseData = PCSCase.builder()
            .discretionaryGroundsWales(discretionaryGrounds)
            .secureContractDiscretionaryGroundsWales(secureDiscretionaryGrounds)
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        PCSCase updatedCaseData = response.getData();
        assertThat(updatedCaseData.getShowASBQuestionsPageWales()).isEqualTo(expectedShowASBQuestionsPage);
    }

    private static Stream<Arguments> asbRoutingScenarios() {
        return Stream.of(
            // ASB in discretionaryGroundsWales only - should show ASB questions page
            arguments(
                Set.of(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_SECTION_157),
                null,
                YesOrNo.YES
            ),
            // ASB in secureContractDiscretionaryGroundsWales only - should show ASB questions page
            arguments(
                null,
                Set.of(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR),
                YesOrNo.YES
            ),
            // ASB with other grounds in discretionaryGroundsWales - should show ASB questions page
            arguments(
                Set.of(
                    DiscretionaryGroundWales.RENT_ARREARS_SECTION_157,
                    DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_SECTION_157
                ),
                null,
                YesOrNo.YES
            ),
            // ASB with other grounds in secureContractDiscretionaryGroundsWales - should show ASB questions page
            arguments(
                null,
                Set.of(
                    SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT,
                    SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR
                ),
                YesOrNo.YES
            ),
            // Only non-ASB in discretionaryGroundsWales - should not show ASB questions page
            arguments(
                Set.of(DiscretionaryGroundWales.RENT_ARREARS_SECTION_157),
                null,
                YesOrNo.NO
            ),
            // Only non-ASB in secureContractDiscretionaryGroundsWales - should not show ASB questions page
            arguments(
                null,
                Set.of(SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT),
                YesOrNo.NO
            ),
            // Both grounds null - should not show ASB questions page
            arguments(
                null,
                null,
                YesOrNo.NO
            )
        );
    }
}
