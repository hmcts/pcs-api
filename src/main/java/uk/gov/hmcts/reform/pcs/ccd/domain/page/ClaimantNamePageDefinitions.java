package uk.gov.hmcts.reform.pcs.ccd.domain.page;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Data
@Builder
public class ClaimantNamePageDefinitions {

    @CCD(
        label = "Claimant Name",
        access = {CitizenAccess.class}
    )
    @External
    private String claimantName;

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
