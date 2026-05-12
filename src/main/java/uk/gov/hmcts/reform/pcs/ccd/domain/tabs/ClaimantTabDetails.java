package uk.gov.hmcts.reform.pcs.ccd.domain.tabs;

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
public class ClaimantTabDetails {

    @CCD(
        label = "Name"
    )
    private String name;

    @CCD(
        label = "Service address"
    )
    private AddressUK serviceAddress;

    @CCD(
        label = "Email address"
    )
    private String emailAddress;

    @CCD(
        label = "Telephone number"
    )
    private String telephoneNumber;
}
