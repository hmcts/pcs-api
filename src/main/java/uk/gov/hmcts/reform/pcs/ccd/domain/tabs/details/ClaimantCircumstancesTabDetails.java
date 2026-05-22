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
public class ClaimantCircumstancesTabDetails {

    @CCD(label = "Is there any information you’d like to provide about the claimant's circumstances?")
    private String claimantCircumstancesGiven;

    @CCD(label = "Claimant circumstances")
    private String claimantCircumstancesDetails;
}
