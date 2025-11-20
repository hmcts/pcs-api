package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimantInformationDetails {

    @CCD(
        label = "Claimant Name",
        access = {CitizenAccess.class}
    )
    @External
    private String claimantName;

    @CCD(
        label = "Organisation Name"
    )
    @External
    private String organisationName;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class}
    )
    private VerticalYesNo isClaimantNameCorrect;

    @CCD(
        access = {CitizenAccess.class}
    )
    private String overriddenClaimantName;
}
