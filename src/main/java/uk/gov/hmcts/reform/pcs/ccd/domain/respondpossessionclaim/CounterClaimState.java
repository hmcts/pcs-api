package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DefendantAccess;

/**
 * Counter-claim lifecycle states on case data.
 */
public enum CounterClaimState {

    @CCD(
        label = "Pending Counterclaim Issued",
        access = {CitizenAccess.class, DefendantAccess.class}
    )
    PENDING_COUNTER_CLAIM_ISSUED,

    @CCD(
        label = "Counterclaim Issued",
        access = {CitizenAccess.class, DefendantAccess.class}
    )
    COUNTER_CLAIM_ISSUED,

}
