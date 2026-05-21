package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimTabDetails {
    @CCD(label = "Claimant type")
    private String claimantType;

    @CCD(label = "Claim against trespassers?")
    private String trespassClaim;
}
