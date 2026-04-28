package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;


// A named party on the claim, used for multi-party routing and display in the counterclaim journey.
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimParty {

    @CCD(access = {CitizenAccess.class})
    private String firstName;

    @CCD(access = {CitizenAccess.class})
    private String lastName;

    @CCD(access = {CitizenAccess.class})
    private String orgName;

    // One of the possible roles: CLAIMANT, DEFENDANT, or UNDERLESSEE_OR_MORTGAGEE.
    @CCD(access = {CitizenAccess.class})
    private String role;

}
