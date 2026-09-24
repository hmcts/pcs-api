package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.parties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepresentativeTabDetails {

    @CCD(label = "Telephone number")
    private String telephoneNumber;

    @CCD(label = "Email address")
    private String emailAddress;

    @CCD (label = "Organisation")
    private OrganisationTabDetails organisation;

}
