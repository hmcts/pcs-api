package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnderlesseeOrMortgageInformationTabDetails {

    @CCD(label = "Underlessee or mortgagee’s name known?")
    private String nameKnown;

    @CCD(label = "Name")
    private String name;

    @CCD(label = "Underlessee or mortgagee’s address for service known?")
    private String addressKnown;

    @CCD(label = "Underlessee or mortgagee address for service")
    private AddressUK address;
}
