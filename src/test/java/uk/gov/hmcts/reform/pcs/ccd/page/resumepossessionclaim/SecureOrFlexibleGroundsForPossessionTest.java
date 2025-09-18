package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.SecureOrFlexibleGroundsForPossession;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;

public class SecureOrFlexibleGroundsForPossessionTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new SecureOrFlexibleGroundsForPossession());
    }

    @ParameterizedTest
    @MethodSource("groundsScenarios")
    void shouldSetSelectedGroundsAndShowReasonsFlag(
            Set<SecureOrFlexibleDiscretionaryGrounds> discretionaryGrounds,
            Set<SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm> discretionaryGroundsAlt,
            Set<SecureOrFlexibleMandatoryGrounds> mandatoryGrounds,
            Set<SecureOrFlexibleMandatoryGroundsAlternativeAccomm> mandatoryGroundsAlt,
            boolean expectError,
            YesOrNo expectedShowReasonsPage) {

        // Given
        PCSCase caseData = PCSCase.builder()
                .secureOrFlexibleDiscretionaryGrounds(discretionaryGrounds)
                .secureOrFlexibleDiscretionaryGroundsAlt(discretionaryGroundsAlt)
                .secureOrFlexibleMandatoryGrounds(mandatoryGrounds)
                .secureOrFlexibleMandatoryGroundsAlt(mandatoryGroundsAlt)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        PCSCase updatedCaseData = response.getData();

        // Then
        if (expectError) {
            assertThat(response.getErrors()).containsExactly("Please select at least one ground");
        } else {

            assertThat(updatedCaseData.getShowReasonsForGroundsPage()).isEqualTo(expectedShowReasonsPage);

            if (!discretionaryGrounds.contains(RENT_ARREARS_OR_BREACH_OF_TENANCY)) {
                assertThat(updatedCaseData.getRentArrearsOrBreachOfTenancy()).isEmpty();
            }
        }
    }

    private static Stream<Arguments> groundsScenarios() {
        return Stream.of(
                //Only one discretionary ground
                arguments(
                        Set.of(SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        YesOrNo.YES
                ),
                // No grounds selected > expect error
                arguments(
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        true,
                        null
                ),
                // Mixed discretionary and mandatory grounds
                arguments(
                        Set.of(SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE),
                        Set.of(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD),
                        Set.of(),
                        Set.of(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.OVERCROWDING),
                        false,
                        YesOrNo.YES
                ),
                // Only mandatory grounds
                arguments(
                        Set.of(),
                        Set.of(),
                        Set.of(SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL),
                        Set.of(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.CHARITABLE_LANDLORD),
                        false,
                        YesOrNo.YES
                ),
                // Only RENT_ARREARS_OR_BREACH_OF_TENANCY > showReasonsForGroundsPage = NO
                arguments(
                        Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        YesOrNo.NO
                )
        );
    }
}
