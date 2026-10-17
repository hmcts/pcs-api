package uk.gov.hmcts.reform.pcs.enforcement.exception;

import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;

import java.util.UUID;

public class IllegalEnforcementStatusTransitionException extends RuntimeException {

    public IllegalEnforcementStatusTransitionException(UUID enforcementId, EnforcementStatus from,
                                                       EnforcementStatus to) {
        super("Enforcement %s cannot move from %s to %s".formatted(
            enforcementId, from == null ? "no status" : from, to));
    }
}
