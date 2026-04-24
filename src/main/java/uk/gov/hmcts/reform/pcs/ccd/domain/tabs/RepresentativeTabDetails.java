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
public class RepresentativeTabDetails {

    @CCD(
        label = "Representative's full name"
    )
    private String name;

    @CCD(
        label = "Telephone number"
    )
    private String telephoneNumber;

    @CCD(
        label = "Email Address"
    )
    private String emailAddress;

    @CCD(
        label = "Organisation Name"
    )
    private String orgName;

    @CCD(
        label = "Organisation Address"
    )
    private AddressUK addressUK;
}
