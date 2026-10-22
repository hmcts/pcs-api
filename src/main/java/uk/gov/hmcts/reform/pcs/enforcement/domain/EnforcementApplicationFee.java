package uk.gov.hmcts.reform.pcs.enforcement.domain;

import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.util.Optional;

public final class EnforcementApplicationFee {

    private EnforcementApplicationFee() {
    }

    public static boolean isPayable(SelectEnforcementType enforcementType) {
        return findFeeType(enforcementType).isPresent();
    }

    public static Optional<FeeType> findFeeType(SelectEnforcementType enforcementType) {
        return switch (enforcementType) {
            case WARRANT -> Optional.of(FeeType.ENFORCEMENT_WARRANT_FEE);
            case WRIT -> Optional.of(FeeType.ENFORCEMENT_WRIT_FEE);
            case WARRANT_OF_RESTITUTION, WRIT_OF_RESTITUTION -> Optional.empty();
        };
    }

}
