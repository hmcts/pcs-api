package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.parties;

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
public class OrganisationTabDetails {

    @CCD(label = "Name")
    private String name;

    @CCD(label = "Address")
    private AddressUK address;

}
