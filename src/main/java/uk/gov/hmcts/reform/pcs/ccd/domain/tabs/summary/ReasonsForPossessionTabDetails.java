package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReasonsForPossessionTabDetails {

    @CCD(
        label = "Reasons for claiming possession under ground X"
    )
    private String groundReasons;

    @CCD(
        label = "Additional reasons for possession"
    )
    private String additionalReasonsForPossession;
}
