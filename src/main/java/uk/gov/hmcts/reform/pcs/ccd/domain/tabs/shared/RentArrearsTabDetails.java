package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentArrearsTabDetails {

    @CCD(
        label = "Rent amount"
    )
    private String rentAmount;

    @CCD(
        label = "How rent is calculated"
    )
    private String calculationFrequency;

    @CCD(
        label = "Daily rate"
    )
    private String dailyRate;

    @CCD(
        label = "Rent arrears total at the time of claim issue"
    )
    private String arrearsTotal;

    @CCD(
        label = "Judgment requested for the outstanding arrears?"
    )
    private String judgmentRequested;
}
