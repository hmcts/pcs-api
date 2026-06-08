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
public class OccupationContractOrLicenceTabDetails {

    @CCD(label = "Occupation contract or licence agreement type")
    private String agreementType;

    @CCD(label = "Description of contract type")
    private String agreementTypeDescription;

    @CCD(label = "Occupation contract or licence start date")
    private String agreementStartDate;
}
