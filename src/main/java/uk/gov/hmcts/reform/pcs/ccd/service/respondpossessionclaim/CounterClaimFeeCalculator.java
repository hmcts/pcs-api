package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.math.BigDecimal;

@Service
public class CounterClaimFeeCalculator {

    private static final BigDecimal RANGED_FEE_UPPER_BOUND = new BigDecimal("5000");

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

        BigDecimal claimAmount = getCounterClaimAmount(counterClaim);
        if (claimAmount == null || claimAmount.signum() < 0) {
            return FeeType.COUNTER_CLAIM;
        }
        if (claimAmount.compareTo(RANGED_FEE_UPPER_BOUND) <= 0) {
            return FeeType.COUNTER_CLAIM_RANGED;
        }
        return FeeType.COUNTER_CLAIM;
    }

    private BigDecimal getCounterClaimAmount(CounterClaim counterClaim) {
        if (counterClaim.getIsClaimAmountKnown() == VerticalYesNo.YES) {
            return counterClaim.getClaimAmount();
        }
        if (counterClaim.getIsClaimAmountKnown() == VerticalYesNo.NO) {
            return counterClaim.getEstimatedMaxClaimAmount();
        }
        return null;
    }
}
