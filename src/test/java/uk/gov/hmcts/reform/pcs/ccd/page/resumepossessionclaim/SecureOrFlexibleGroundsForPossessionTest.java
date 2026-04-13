package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureAntisocialAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;

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
            Set<SecureAntisocialAdditionalGrounds> secureAntisocialAdditionalGrounds,
            boolean expectNoGroundsSelectedError,
            boolean expectAntisocialAdditionalGroundsError,
            YesOrNo expectedShowReasonsPage) {

        SecureOrFlexiblePossessionGrounds secureOrFlexibleGrounds = SecureOrFlexiblePossessionGrounds.builder()
                .secureOrFlexibleDiscretionaryGrounds(discretionaryGrounds)
                .secureOrFlexibleDiscretionaryGroundsAlt(discretionaryGroundsAlt)
                .secureOrFlexibleMandatoryGrounds(mandatoryGrounds)
                .secureOrFlexibleMandatoryGroundsAlt(mandatoryGroundsAlt)
                .secureAntisocialAdditionalGrounds(secureAntisocialAdditionalGrounds)
                .build();
        // Given
        PCSCase caseData = PCSCase.builder()
                .secureOrFlexiblePossessionGrounds(secureOrFlexibleGrounds)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        PCSCase updatedCaseData = response.getData();

        // Then
        if (expectNoGroundsSelectedError) {
            assertThat(response.getErrorMessageOverride()).contains("Please select at least one ground");
        } else if (expectAntisocialAdditionalGroundsError) {
            assertThat(response.getErrorMessageOverride())
                    .contains("Please select at least one absolute ground for possession for antisocial");
        } else {
            assertThat(updatedCaseData.getShowReasonsForGroundsPage()).isEqualTo(expectedShowReasonsPage);

            if (!discretionaryGrounds.contains(RENT_ARREARS_OR_BREACH_OF_TENANCY)) {
                assertThat(updatedCaseData.getRentArrearsOrBreachOfTenancy()).isEmpty();
            }
        }
    }

    private static Stream<Arguments> groundsScenarios() {
        return Stream.of(
                //Discretionary ground
                arguments(
                        Set.of(SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        false,
                        YesOrNo.YES
                ),
                //Discretionary Alt ground
                arguments(
                        Set.of(),
                        Set.of(SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ADAPTED_ACCOMMODATION),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        false,
                        YesOrNo.YES
                ),
                //Mandatory Alt ground
                arguments(
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.CHARITABLE_LANDLORD),
                        Set.of(),
                        false,
                        false,
                        YesOrNo.YES
                ),
                // No grounds selected > expect error
                arguments(
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        true,
                        false,
                        null
                ),
                // Mixed discretionary and mandatory grounds
                arguments(
                        Set.of(SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE),
                        Set.of(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD),
                        Set.of(),
                        Set.of(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.OVERCROWDING),
                        Set.of(),
                        false,
                        false,
                        YesOrNo.YES
                ),
                // Only mandatory grounds
                arguments(
                        Set.of(),
                        Set.of(),
                        Set.of(SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL),
                        Set.of(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.CHARITABLE_LANDLORD),
                        Set.of(SecureAntisocialAdditionalGrounds.S84A_CONDITION_1),
                        false,
                        false,
                        YesOrNo.YES
                ),
                // Only mandatory grounds with no absolute additional ground
                arguments(
                        Set.of(),
                        Set.of(),
                        Set.of(SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL),
                        Set.of(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.CHARITABLE_LANDLORD),
                        Set.of(),
                        false,
                        true,
                        YesOrNo.YES
                ),
                // Only RENT_ARREARS_OR_BREACH_OF_TENANCY > showReasonsForGroundsPage = NO
                arguments(
                        Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        false,
                        YesOrNo.NO
                )
        );
    }

}
