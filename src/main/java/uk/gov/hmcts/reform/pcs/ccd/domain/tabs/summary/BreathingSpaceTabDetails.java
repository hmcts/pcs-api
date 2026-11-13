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
public class BreathingSpaceTabDetails {

    @CCD(label = "Reference number")
    private String referenceNumber;

    @CCD(label = "Start date")
    private String startDate;

    @CCD(label = "Type")
    private String type;

    @CCD(label = "Expected end date")
    private String expectedEndDate;
}
