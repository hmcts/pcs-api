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
            .estimatedMaxClaimAmount(new BigDecimal("2500.00"))
            .build();

        assertThat(underTest.resolveFeeType(counterClaim)).isEqualTo(FeeType.COUNTER_CLAIM_RANGED);
    }

    @Test
    void shouldResolveFeeLookupAmountInPoundsFromClaimAmount() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(new BigDecimal("2500.00"))
            .build();

        assertThat(underTest.resolveFeeLookupAmountInPounds(counterClaim))
            .isEqualByComparingTo(new BigDecimal("2500.00"));
    }

    @Test
    void shouldNotRequirePaymentWhenHwfReferenceIsPresent() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .hwfReferenceNumber("HWF-123-456")
            .build();

        assertThat(underTest.isPaymentRequired(counterClaim)).isFalse();
    }

    @Test
    void shouldRequirePaymentWhenHwfReferenceIsMissing() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .build();

        assertThat(underTest.isPaymentRequired(counterClaim)).isTrue();
    }

    @Test
    void shouldThrowWhenClaimTypeIsMissing() {
        assertThatThrownBy(() -> underTest.resolveFeeType(CounterClaim.builder().build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("claim type");
    }

    @Test
    void shouldThrowWhenCounterClaimIsNull() {
        assertThatThrownBy(() -> underTest.resolveFeeType(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("claim type");
    }

    @Test
    void shouldNotRequirePaymentWhenCounterClaimIsNull() {
        assertThat(underTest.isPaymentRequired(null)).isFalse();
    }

    @Test
    void shouldRequirePaymentWhenHwfReferenceIsBlank() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .hwfReferenceNumber("   ")
            .build();

        assertThat(underTest.isPaymentRequired(counterClaim)).isTrue();
    }

    @Test
    void shouldUseStandardFeeWhenClaimAmountIsUnknown() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.NO)
            .build();

        assertThat(underTest.resolveFeeType(counterClaim)).isEqualTo(FeeType.COUNTER_CLAIM);
    }

    @Test
    void shouldUseStandardFeeWhenClaimAmountIsNegative() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(new BigDecimal("-100"))
            .build();

        assertThat(underTest.resolveFeeType(counterClaim)).isEqualTo(FeeType.COUNTER_CLAIM);
    }

    @Test
    void shouldResolveFeeLookupAmountInPoundsFromEstimatedMaxAmount() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.NO)
            .estimatedMaxClaimAmount(new BigDecimal("100.50"))
            .build();

        assertThat(underTest.resolveFeeLookupAmountInPounds(counterClaim))
            .isEqualByComparingTo(new BigDecimal("100.50"));
    }

    private static Stream<Arguments> knownAmountFeeScenarios() {
        return Stream.of(
            Arguments.of(new BigDecimal("5000.00"), FeeType.COUNTER_CLAIM_RANGED),
            Arguments.of(new BigDecimal("5001.00"), FeeType.COUNTER_CLAIM)
        );
    }
}
