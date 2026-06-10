package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

public record RespondPossessionClaimSubmitPersistenceResult(
    PossessionClaimResponse possessionClaimResponse,
    CounterClaimEntity counterClaimEntity,
    boolean issuedWithoutPayment
) {
}
