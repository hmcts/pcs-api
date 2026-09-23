package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

public record RespondPossessionClaimSubmitPersistenceResult(
    PossessionClaimResponse possessionClaimResponse,
    CounterClaimEntity counterClaimEntity,
    FeeDetails feeDetails,
    boolean paymentRequired
) {
}
