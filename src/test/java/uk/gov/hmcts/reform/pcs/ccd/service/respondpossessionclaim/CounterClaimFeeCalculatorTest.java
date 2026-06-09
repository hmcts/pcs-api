package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CounterClaimFeeCalculatorTest {

    private final CounterClaimFeeCalculator underTest = new CounterClaimFeeCalculator();

    @Test
    void shouldUseFlatFeeWhenClaimTypeIsSomethingElse() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.SOMETHING_ELSE)
            .build();

        assertThat(underTest.resolveFeeType(counterClaim)).isEqualTo(FeeType.COUNTER_CLAIM_FLAT_FEE);
    }

    @ParameterizedTest
    @MethodSource("knownAmountFeeScenarios")
    void shouldResolveFeeTypeForKnownAmounts(
        BigDecimal claimAmount,
        FeeType expectedFeeType
    ) {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(claimAmount)
            .build();

        assertThat(underTest.resolveFeeType(counterClaim)).isEqualTo(expectedFeeType);
    }

    @Test
    void shouldUseEstimatedMaxAmountWhenClaimAmountIsUnknown() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.NO)
            .estimatedMaxClaimAmount(new BigDecimal("2500"))
            .build();

        assertThat(underTest.resolveFeeType(counterClaim)).isEqualTo(FeeType.COUNTER_CLAIM_RANGED);
    }

    @Test
    void shouldThrowWhenClaimTypeIsMissing() {
        assertThatThrownBy(() -> underTest.resolveFeeType(CounterClaim.builder().build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("claim type");
    }

    private static Stream<Arguments> knownAmountFeeScenarios() {
        return Stream.of(
            Arguments.of(new BigDecimal("5000"), FeeType.COUNTER_CLAIM_RANGED),
            Arguments.of(new BigDecimal("5001"), FeeType.COUNTER_CLAIM)
        );
    }
}
