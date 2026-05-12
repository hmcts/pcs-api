package uk.gov.hmcts.reform.pcs.ccd.domain.tabs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionsTakenTabDetails {

    @CCD(
        label = "Pre-action protocol followed?"
    )
    private String preactionProtocolFollowed;

    @CCD(
        label = "Explain why you have not followed the pre-action protocol"
    )
    private String preActionProtocolIncompleteExplanation;

    @CCD(
        label = "Mediation attempted?"
    )
    private String mediationAttempted;

    @CCD(
        label = "Settlement attempted?"
    )
    private String settlementAttempted;
}
