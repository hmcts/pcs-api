package uk.gov.hmcts.reform.pcs.enforcement.domain;

import java.util.Optional;
import java.util.function.Function;

/**
 * Which enforcement types the claimant pays a fee to apply for, and where that fee amount sits on the case
 * data. Only a warrant and a writ of possession have a fee in pcs-api (FEE0380 and FEE0397 via
 * {@code FeeType.ENFORCEMENT_WARRANT_FEE} and {@code FeeType.ENFORCEMENT_WRIT_FEE}); neither restitution type
 * has a fee code or a fee amount.
 *
 * <p>Both the confirmation screen's payment panel and the status a submitted application starts in come from
 * the single mapping below, so they cannot disagree about whether a type is paid for.</p>
 */
public final class EnforcementApplicationFee {

    private EnforcementApplicationFee() {
    }

    /**
     * Whether applying for this type owes a fee. This is a property of the type, not of the submitted case
     * data, so it is the only safe way to decide whether an application is awaiting payment.
     */
    public static boolean isPayable(SelectEnforcementType enforcementType) {
        return feeAmountOnCaseData(enforcementType).isPresent();
    }

    /**
     * The fee amount to show the claimant, or empty when the type has no fee or no amount was looked up. The
     * amount is put on the case data by the event's start handler and comes back from the client, so it must
     * not be used to decide whether a fee is owed.
     */
    public static Optional<String> findAmount(EnforcementOrder enforcementOrder,
                                              SelectEnforcementType enforcementType) {
        return feeAmountOnCaseData(enforcementType).map(amount -> amount.apply(enforcementOrder));
    }

    @SuppressWarnings("deprecation")
    private static Optional<Function<EnforcementOrder, String>> feeAmountOnCaseData(
        SelectEnforcementType enforcementType) {

        return switch (enforcementType) {
            case WARRANT -> Optional.of(EnforcementOrder::getWarrantFeeAmount);
            case WRIT -> Optional.of(EnforcementOrder::getWritFeeAmount);
            case WARRANT_OF_RESTITUTION, WRIT_OF_RESTITUTION -> Optional.empty();
        };
    }

}
