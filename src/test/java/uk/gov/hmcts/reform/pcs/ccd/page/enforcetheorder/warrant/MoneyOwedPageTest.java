package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.FeeValidationService;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MoneyOwedPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        FeeValidationService feeValidationService = new FeeValidationService();
        setPageUnderTest(new MoneyOwedPage(feeValidationService));
    }

    @ParameterizedTest
    @MethodSource("validFees")
    void shouldAcceptValidFee(BigDecimal validFees) {
        // Given
        MoneyOwedByDefendants moneyOwedByDefendants = MoneyOwedByDefendants.builder()
            .amountOwed(validFees)
            .build();

        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .warrantDetails(WarrantDetails.builder()
                                                      .moneyOwedByDefendants(moneyOwedByDefendants)
                                                      .build())
                                  .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder().getWarrantDetails()
                       .getMoneyOwedByDefendants().getAmountOwed()).isEqualTo(validFees);
    }

    @ParameterizedTest
    @MethodSource("invalidFees")
    void shouldShouldFees(BigDecimal invalidFee) {
        // Given
        MoneyOwedByDefendants moneyOwedByDefendants = MoneyOwedByDefendants.builder()
            .amountOwed(invalidFee)
            .build();

        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .warrantDetails(WarrantDetails.builder()
                                                      .moneyOwedByDefendants(moneyOwedByDefendants)
                                                      .build())
                                  .build())
            .build();
        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        String expectedError = "Money owed should be more than 0.01";

        assertThat(response.getErrors()).containsExactly(expectedError);
        assertThat(response.getData().getEnforcementOrder().getWarrantDetails()
                       .getMoneyOwedByDefendants().getAmountOwed()).isEqualTo(invalidFee);
    }

    private static Stream<Arguments> validFees() {
        return Stream.of(
            Arguments.of(new BigDecimal("100.00")),
            Arguments.of(new BigDecimal("1000000000")),
            Arguments.of(new BigDecimal("998999")),
            Arguments.of(BigDecimal.TEN)
        );
    }

    private static Stream<Arguments> invalidFees() {
        return Stream.of(
            Arguments.of(new BigDecimal("-100.00")),
            Arguments.of(new BigDecimal("100000000000000.00")),
            Arguments.of(new BigDecimal("10001999999999999")),
            Arguments.of(BigDecimal.ZERO)
        );
    }
}

