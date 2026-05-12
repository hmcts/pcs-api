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
public class GroundsForPossessionTabDetails {

    @CCD(
        label = "Grounds"
    )
    private String grounds;

    @CCD(
        label = "Description of other grounds"
    )
    private String otherGroundsDescription;
}
