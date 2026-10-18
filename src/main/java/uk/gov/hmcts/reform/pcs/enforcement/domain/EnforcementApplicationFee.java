package uk.gov.hmcts.reform.pcs.enforcement.domain;

import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.util.Optional;

/**
 * Which enforcement types the claimant pays a fee to apply for, and which fee. Only a warrant and a writ of
 * possession have a fee in pcs-api (FEE0380 via {@link FeeType#ENFORCEMENT_WARRANT_FEE} and FEE0397 via
 * {@link FeeType#ENFORCEMENT_WRIT_FEE}); neither restitution type has a fee code.
 *
 * <p>The status a submitted application starts in, the Payments service request raised for it and the
 * confirmation screen's payment panel all come from the single mapping below, so they cannot disagree about
 * whether a type is paid for. A free repeat warrant (HDPI-8424) is a payable type whose fee is waived:
 * EnforcementOrderService decides that.</p>
 */
public final class EnforcementApplicationFee {

    private EnforcementApplicationFee() {
    }

    /**
     * Whether applying for this type owes a fee. This is a property of the type, not of the submitted case
     * data, so it is the only safe way to decide whether an application is awaiting payment.
     */
    public static boolean isPayable(SelectEnforcementType enforcementType) {
        return findFeeType(enforcementType).isPresent();
    }

    /**
     * The Fees Register fee for applying for this type, or empty when the type has no fee.
     */
    public static Optional<FeeType> findFeeType(SelectEnforcementType enforcementType) {
        return switch (enforcementType) {
            case WARRANT -> Optional.of(FeeType.ENFORCEMENT_WARRANT_FEE);
            case WRIT -> Optional.of(FeeType.ENFORCEMENT_WRIT_FEE);
            case WARRANT_OF_RESTITUTION, WRIT_OF_RESTITUTION -> Optional.empty();
        };
    }

}
