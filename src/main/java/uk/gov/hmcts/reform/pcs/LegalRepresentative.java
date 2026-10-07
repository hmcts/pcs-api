package uk.gov.hmcts.reform.pcs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalRepresentative {

    private String telephoneNumber;

    private String emailAddress;

    private String organisationName;

    private AddressUK address;

}
