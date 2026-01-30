package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementOfTruthPageTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @InjectMocks
    private StatementOfTruthPage statementOfTruthPage;

    @BeforeEach
    void setUp() {
        setPageUnderTest(statementOfTruthPage);
    }

    @Nested
    @DisplayName("Character limit validation")
    class CharacterLimitValidation {

        @ParameterizedTest
        @MethodSource("characterLimitScenarios")
        void shouldValidateCharacterLimits(String fieldName, String value, boolean shouldHaveError) {
            // Given
            StatementOfTruthDetails statementOfTruth = StatementOfTruthDetails.builder()
                .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
                .fullNameClaimant(fieldName.equals("Full name") ? value : "Valid Name")
                .positionClaimant(fieldName.equals("Position or office held") ? value : "Valid Position")
                .build();

            PCSCase caseData = createCaseDataWithStatementOfTruth(statementOfTruth);

            doAnswer(invocation -> {
                String fieldValue = invocation.getArgument(0);
                String fieldLabel = invocation.getArgument(1);
                int maxCharacters = invocation.getArgument(2);
                List<String> errors = invocation.getArgument(3);
                if (fieldValue != null && fieldValue.length() > maxCharacters) {
                    errors.add(String.format(
                        "In '%s', you have entered more than the maximum number of characters (%d)",
                        fieldLabel, maxCharacters));
                }
                return null;
            }).when(textAreaValidationService).validateTextArea(any(), any(), anyInt(), any());
            when(textAreaValidationService.createValidationResponse(any(), any()))
                .thenAnswer(invocation -> {
                    List<String> errors = invocation.getArgument(1);
                    return createResponseWithErrors(caseData, errors);
                });

            // When
            var response = callMidEventHandler(caseData);

            // Then
            if (shouldHaveError) {
                assertThat(response.getErrors()).isNotEmpty();
            } else {
                verify(textAreaValidationService).validateTextArea(
                    eq(value),
                    eq(fieldName),
                    eq(TextAreaValidationService.STATEMENT_OF_TRUTH_CHARACTER_LIMIT),
                    any()
                );
            }
        }

        private static Stream<Arguments> characterLimitScenarios() {
            return Stream.of(
                Arguments.of("Full name", "A".repeat(61), true),
                Arguments.of("Full name", "A".repeat(60), false),
                Arguments.of("Position or office held", "B".repeat(61), true),
                Arguments.of("Position or office held", "B".repeat(60), false)
            );
        }
    }

    private PCSCase createCaseDataWithStatementOfTruth(StatementOfTruthDetails statementOfTruth) {
        StatementOfTruthDetails sot = statementOfTruth != null
            ? statementOfTruth
            : StatementOfTruthDetails.builder().build();

        WarrantDetails warrantDetails = WarrantDetails.builder()
            .isSuspendedOrder(null)
            .statementOfTruth(sot)
            .repaymentCosts(RepaymentCosts.builder()
                .repaymentSummaryMarkdown("<table>Payments Due Table</table>")
                .build())
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantDetails(warrantDetails)
            .build();

        return PCSCase.builder()
            .enforcementOrder(enforcementOrder)
            .build();
    }

    private uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse<PCSCase, State> createResponseWithErrors(
        PCSCase caseData, List<String> errors) {
        return uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errors(errors != null && !errors.isEmpty() ? errors : null)
            .build();
    }
}

