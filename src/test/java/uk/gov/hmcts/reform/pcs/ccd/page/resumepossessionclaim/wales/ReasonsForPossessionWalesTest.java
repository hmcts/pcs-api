package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ReasonsForPossessionWalesTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        lenient().doReturn(new ArrayList<>()).when(textAreaValidationService)
            .validateMultipleTextAreas(any(), any());
        doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), anyList());

        setPageUnderTest(new ReasonsForPossessionWales(textAreaValidationService));
    }

    @ParameterizedTest
    @MethodSource("asbRoutingScenarios")
    void shouldSetShowASBQuestionsPageWalesBasedOnGroundsSelection(
        Set<DiscretionaryGroundWales> discretionaryGrounds,
        Set<SecureContractDiscretionaryGroundsWales> secureDiscretionaryGrounds,
        YesOrNo expectedShowASBQuestionsPage) {

        PCSCase caseData = PCSCase.builder()
            .groundsForPossessionWales(GroundsForPossessionWales.builder()
                .discretionaryGroundsWales(discretionaryGrounds)
                .build())
            .secureContractGroundsForPossessionWales(
                SecureContractGroundsForPossessionWales.builder()
                    .discretionaryGroundsWales(secureDiscretionaryGrounds)
                    .build()
            )
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

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Should validate all Wales grounds reasons when provided")
        void shouldValidateAllWalesGroundsReasonsWhenProvided() {
            PCSCase caseData = PCSCase.builder()
                .groundsReasonsWales(GroundsReasonsWales.builder()
                    .failToGiveUpS170Reason("Failure to give up S170 reason")
                    .landlordNoticePeriodicS178Reason("Landlord notice periodic S178 reason")
                    .seriousArrearsPeriodicS181Reason("Serious arrears periodic S181 reason")
                    .landlordNoticeFtEndS186Reason("Landlord notice FT end S186 reason")
                    .seriousArrearsFixedTermS187Reason("Serious arrears fixed term S187 reason")
                    .failToGiveUpBreakNoticeS191Reason("Failure to give up break notice S191 reason")
                    .landlordBreakClauseS199Reason("Landlord break clause S199 reason")
                    .convertedFixedTermSch1225B2Reason("Converted fixed term Sch12 25B2 reason")
                    .otherBreachSection157Reason("Other breach section 157 reason")
                    .buildingWorksReason("Building works reason")
                    .redevelopmentSchemesReason("Redevelopment schemes reason")
                    .charitiesReason("Charities reason")
                    .disabledSuitableDwellingReason("Disabled suitable dwelling reason")
                    .housingAssociationsAndTrustsReason("Housing associations and trusts reason")
                    .specialNeedsDwellingsReason("Special needs dwellings reason")
                    .reserveSuccessorsReason("Reserve successors reason")
                    .jointContractHoldersReason("Joint contract holders reason")
                    .otherEstateManagementReasonsReason("Other estate management reasons reason")
                    .secureFailureToGiveUpPossessionSection170Reason("Secure failure S170 reason")
                    .secureLandlordNoticeSection186Reason("Secure landlord notice S186 reason")
                    .secureFailureToGiveUpPossessionSection191Reason("Secure failure S191 reason")
                    .secureLandlordNoticeSection199Reason("Secure landlord notice S199 reason")
                    .secureOtherBreachOfContractReason("Secure other breach reason")
                    .secureBuildingWorksReason("Secure building works reason")
                    .secureRedevelopmentSchemesReason("Secure redevelopment schemes reason")
                    .secureCharitiesReason("Secure charities reason")
                    .secureDisabledSuitableDwellingReason("Secure disabled suitable dwelling reason")
                    .secureHousingAssociationsAndTrustsReason("Secure housing associations reason")
                    .secureSpecialNeedsDwellingsReason("Secure special needs dwellings reason")
                    .secureReserveSuccessorsReason("Secure reserve successors reason")
                    .secureJointContractHoldersReason("Secure joint contract holders reason")
                    .secureOtherEstateManagementReasonsReason("Secure other estate management reason")
                    .build())
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle null grounds reasons Wales gracefully")
        void shouldHandleNullGroundsReasonsWalesGracefully() {
            PCSCase caseData = PCSCase.builder()
                .groundsReasonsWales(null)
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle empty grounds reasons Wales gracefully")
        void shouldHandleEmptyGroundsReasonsWalesGracefully() {
            PCSCase caseData = PCSCase.builder()
                .groundsReasonsWales(GroundsReasonsWales.builder().build())
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should handle partial grounds reasons Wales gracefully")
        void shouldHandlePartialGroundsReasonsWalesGracefully() {
            PCSCase caseData = PCSCase.builder()
                .groundsReasonsWales(GroundsReasonsWales.builder()
                    .failToGiveUpS170Reason("Only failure to give up S170 reason")
                    .build())
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should validate standard contract mandatory grounds reasons")
        void shouldValidateStandardContractMandatoryGroundsReasons() {
            PCSCase caseData = PCSCase.builder()
                .groundsReasonsWales(GroundsReasonsWales.builder()
                    .failToGiveUpS170Reason("Failure to give up S170 reason")
                    .landlordNoticePeriodicS178Reason("Landlord notice periodic S178 reason")
                    .seriousArrearsPeriodicS181Reason("Serious arrears periodic S181 reason")
                    .landlordNoticeFtEndS186Reason("Landlord notice FT end S186 reason")
                    .seriousArrearsFixedTermS187Reason("Serious arrears fixed term S187 reason")
                    .failToGiveUpBreakNoticeS191Reason("Failure to give up break notice S191 reason")
                    .landlordBreakClauseS199Reason("Landlord break clause S199 reason")
                    .convertedFixedTermSch1225B2Reason("Converted fixed term Sch12 25B2 reason")
                    .build())
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should validate secure contract grounds reasons")
        void shouldValidateSecureContractGroundsReasons() {
            PCSCase caseData = PCSCase.builder()
                .groundsReasonsWales(GroundsReasonsWales.builder()
                    .secureFailureToGiveUpPossessionSection170Reason("Secure failure S170 reason")
                    .secureLandlordNoticeSection186Reason("Secure landlord notice S186 reason")
                    .secureFailureToGiveUpPossessionSection191Reason("Secure failure S191 reason")
                    .secureLandlordNoticeSection199Reason("Secure landlord notice S199 reason")
                    .secureOtherBreachOfContractReason("Secure other breach reason")
                    .build())
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should validate estate management grounds reasons")
        void shouldValidateEstateManagementGroundsReasons() {
            PCSCase caseData = PCSCase.builder()
                .groundsReasonsWales(GroundsReasonsWales.builder()
                    .buildingWorksReason("Building works reason")
                    .redevelopmentSchemesReason("Redevelopment schemes reason")
                    .charitiesReason("Charities reason")
                    .disabledSuitableDwellingReason("Disabled suitable dwelling reason")
                    .housingAssociationsAndTrustsReason("Housing associations and trusts reason")
                    .specialNeedsDwellingsReason("Special needs dwellings reason")
                    .reserveSuccessorsReason("Reserve successors reason")
                    .jointContractHoldersReason("Joint contract holders reason")
                    .otherEstateManagementReasonsReason("Other estate management reasons reason")
                    .build())
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getData()).isEqualTo(caseData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }
    }
}


