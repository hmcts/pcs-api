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

    @CCD(label = "What type of occupation contract or licence is in place?")
    private String agreementType;

    @CCD(label = "What date did the occupation contract or licence begin?")
    private String agreementStartDate;
}
