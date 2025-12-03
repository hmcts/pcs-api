package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

public class SecureContractGroundsForPossessionWalesTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new SecureContractGroundsForPossessionWalesPage());
    }

    @ParameterizedTest
    @MethodSource("groundScenarios")
    void shouldValidateSecureContractWalesGroundInputs(
            Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds,
            Set<SecureContractMandatoryGroundsWales> mandatoryGrounds,
            Set<EstateManagementGroundsWales> estateGrounds,
            boolean expectEstateError,
            boolean expectGroundsError,
            boolean expectValid
    ) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .secureContractGroundsForPossessionWales(
                SecureContractGroundsForPossessionWales.builder()
                .discretionaryGroundsWales(discretionaryGrounds)
                .mandatoryGroundsWales(mandatoryGrounds)
                .estateManagementGroundsWales(estateGrounds)
                .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (expectEstateError) {
            assertThat(response.getErrors()).containsExactly(
                "Please select at least one ground in ‘Estate management grounds (section 160)’.");
        } else if (expectGroundsError) {
            assertThat(response.getErrors()).containsExactly("Please select at least one ground");
        } else if (expectValid) {
            assertThat(response.getErrors()).isNullOrEmpty();
        }
    }

    private static Stream<Arguments> groundScenarios() {
        return Stream.of(
                // No grounds selected - should error on missing grounds
                arguments(
                        Set.of(), Set.of(), Set.of(),
                        false, true, false
                ),
                // ESTATE_MANAGEMENT_GROUNDS selected with no estate details
                // - should error on missing estate management grounds
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS),
                        Set.of(), Set.of(),
                        true, false, false
                ),
                // ESTATE_MANAGEMENT_GROUNDS + BUILDING_WORKS provided - should be valid
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS),
                        Set.of(),
                        Set.of(EstateManagementGroundsWales.BUILDING_WORKS),
                        false, false, true
                ),
                // Only mandatory ground present, should be valid
                arguments(
                        Set.of(),
                        Set.of(SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170),
                        Set.of(),
                        false, false, true
                ),
                // Multiple discretionary (incl ESTATE_MANAGEMENT_GROUNDS)+ valid estate - should be valid
                arguments(
                        Set.of(
                                SecureContractDiscretionaryGroundsWales.RENT_ARREARS,
                                SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS),
                        Set.of(),
                        Set.of(EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES),
                        false, false, true
                ),
                // discretionary non-estate ground (ANTISOCIAL_BEHAVIOUR) - should be valid
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR),
                        Set.of(), Set.of(),
                        false, false, true
                ),
                // Discretionary (OTHER_BREACH_OF_CONTRACT) + mandatory (LANDLORD_NOTICE_SECTION_186) - should be valid
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT),
                        Set.of(SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_SECTION_186),
                        Set.of(),
                        false, false, true
                )
        );
    }

    @ParameterizedTest
    @MethodSource("routingScenarios")
    void shouldSetCorrectRoutingFlags(
            Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds,
            Set<EstateManagementGroundsWales> estateManagementGrounds,
            Set<SecureContractMandatoryGroundsWales> mandatoryGrounds,
            YesOrNo expectedShowASBQuestionsPage,
            YesOrNo expectedShowReasonsForGroundsPage) {

        // Given
        PCSCase caseData = PCSCase.builder()
            .secureContractGroundsForPossessionWales(
                SecureContractGroundsForPossessionWales.builder()
                    .discretionaryGroundsWales(discretionaryGrounds)
                    .estateManagementGroundsWales(estateManagementGrounds)
                    .mandatoryGroundsWales(mandatoryGrounds)
                    .build()
                )
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        PCSCase updatedCaseData = response.getData();
        assertThat(updatedCaseData.getShowASBQuestionsPageWales()).isEqualTo(expectedShowASBQuestionsPage);
        assertThat(updatedCaseData.getShowReasonsForGroundsPageWales()).isEqualTo(expectedShowReasonsForGroundsPage);
    }

    private static Stream<Arguments> routingScenarios() {
        return Stream.of(
                // Only rent arrears - go to pre-action protocol page
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.RENT_ARREARS),
                        Set.of(),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.NO
                ),
                // Only ASB - show ASB questions page
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR),
                        Set.of(),
                        Set.of(),
                        YesOrNo.YES,
                        YesOrNo.NO
                ),
                // Rent arrears + ASB - show ASB questions page (ASB takes precedence)
                arguments(
                        Set.of(
                                SecureContractDiscretionaryGroundsWales.RENT_ARREARS,
                                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR
                        ),
                        Set.of(),
                        Set.of(),
                        YesOrNo.YES,
                        YesOrNo.NO
                ),
                // Only other breach - show reasons for grounds page
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT),
                        Set.of(),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + other breach - show reasons for grounds page
                arguments(
                        Set.of(
                                SecureContractDiscretionaryGroundsWales.RENT_ARREARS,
                                SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT
                        ),
                        Set.of(),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // ASB + other breach - show reasons for grounds page
                arguments(
                        Set.of(
                                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR,
                                SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT
                        ),
                        Set.of(),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + ASB + other breach - show reasons for grounds page
                arguments(
                        Set.of(
                                SecureContractDiscretionaryGroundsWales.RENT_ARREARS,
                                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR,
                                SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT
                        ),
                        Set.of(),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Only estate management grounds - show reasons for grounds page
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS),
                        Set.of(EstateManagementGroundsWales.BUILDING_WORKS),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + estate management - show reasons for grounds page
                arguments(
                        Set.of(
                                SecureContractDiscretionaryGroundsWales.RENT_ARREARS,
                                SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS
                        ),
                        Set.of(EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // ASB + estate management - show reasons for grounds page
                arguments(
                        Set.of(
                                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR,
                                SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS
                        ),
                        Set.of(EstateManagementGroundsWales.CHARITIES),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + ASB + estate management - show reasons for grounds page
                arguments(
                        Set.of(
                                SecureContractDiscretionaryGroundsWales.RENT_ARREARS,
                                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR,
                                SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS
                        ),
                        Set.of(EstateManagementGroundsWales.BUILDING_WORKS),
                        Set.of(),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Only mandatory grounds - show reasons for grounds page
                arguments(
                        Set.of(),
                        Set.of(),
                        Set.of(SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_SECTION_186),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + mandatory grounds - show reasons for grounds page
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.RENT_ARREARS),
                        Set.of(),
                        Set.of(SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // ASB + mandatory grounds - show reasons for grounds page
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR),
                        Set.of(),
                        Set.of(SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_SECTION_199),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Rent arrears + ASB + mandatory grounds - show reasons for grounds page
                arguments(
                        Set.of(
                                SecureContractDiscretionaryGroundsWales.RENT_ARREARS,
                                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR
                        ),
                        Set.of(),
                        Set.of(SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_SECTION_191),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Other breach + mandatory grounds - show reasons for grounds page
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT),
                        Set.of(),
                        Set.of(SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_SECTION_186),
                        YesOrNo.NO,
                        YesOrNo.YES
                ),
                // Estate management + mandatory grounds - show reasons for grounds page
                arguments(
                        Set.of(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS),
                        Set.of(EstateManagementGroundsWales.CHARITIES),
                        Set.of(SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_SECTION_199),
                        YesOrNo.NO,
                        YesOrNo.YES
                )
        );
    }
}
