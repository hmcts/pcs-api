package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CounterClaimFeeCalculator {

    private static final BigDecimal PENCE_PER_POUND = new BigDecimal("100");
    private static final BigDecimal RANGED_FEE_UPPER_BOUND_POUNDS = new BigDecimal("5000");

    public boolean isPaymentRequired(CounterClaim counterClaim) {
        if (counterClaim == null) {
            return false;
        }
        String hwfReference = counterClaim.getHwfReferenceNumber();
        return hwfReference == null || hwfReference.trim().isEmpty();
    }

    public FeeType resolveFeeType(CounterClaim counterClaim) {
        if (counterClaim == null || counterClaim.getClaimType() == null) {
            throw new IllegalStateException("Counterclaim fee type cannot be determined without claim type");
        }
        if (counterClaim.getClaimType() == CounterClaimType.SOMETHING_ELSE) {
            return FeeType.COUNTER_CLAIM_FLAT_FEE;
        }

        BigDecimal claimAmountInPounds = getCounterClaimAmountInPounds(counterClaim);
        if (claimAmountInPounds == null || claimAmountInPounds.signum() < 0) {
            return FeeType.COUNTER_CLAIM;
        }
        if (claimAmountInPounds.compareTo(RANGED_FEE_UPPER_BOUND_POUNDS) <= 0) {
            return FeeType.COUNTER_CLAIM_RANGED;
        }
        return FeeType.COUNTER_CLAIM;
    }

    /**
     * Claim amounts are stored in pence (CCD / frontend). Fees register expects pounds.
     */
    public BigDecimal resolveFeeLookupAmountInPounds(CounterClaim counterClaim) {
        return getCounterClaimAmountInPounds(counterClaim);
    }

    private BigDecimal getCounterClaimAmountInPounds(CounterClaim counterClaim) {
        BigDecimal amountInPence = getCounterClaimAmountInPence(counterClaim);
        if (amountInPence == null) {
            return null;
        }
        return amountInPence.divide(PENCE_PER_POUND, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal getCounterClaimAmountInPence(CounterClaim counterClaim) {
        if (counterClaim.getIsClaimAmountKnown() == VerticalYesNo.YES) {
            return counterClaim.getClaimAmount();
        }
        if (counterClaim.getIsClaimAmountKnown() == VerticalYesNo.NO) {
            return counterClaim.getEstimatedMaxClaimAmount();
        }
        return null;
    }
}
