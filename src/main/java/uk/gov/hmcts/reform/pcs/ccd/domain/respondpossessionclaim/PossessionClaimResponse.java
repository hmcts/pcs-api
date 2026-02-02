package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PossessionClaimResponse {

    @CCD(access = {CitizenAccess.class})
    private DefendantContactDetails defendantContactDetails;

    @CCD(access = {CitizenAccess.class})
    private DefendantResponses defendantResponses;
}

