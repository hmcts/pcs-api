package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Data
@Builder
public class ProhibitedConductWales {

    @CCD(
        label = "Are you also making a claim for an order imposing a prohibited conduct standard contract?"
    )
    private VerticalYesNo claimForProhibitedConductContract;
}
