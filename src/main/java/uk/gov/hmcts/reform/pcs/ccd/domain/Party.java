package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Party {

    private String firstName;

    private String lastName;

    private String orgName;

    private VerticalYesNo nameKnown;

    // emailAddress: NO CitizenAccess annotation - stored in DB but NOT exposed to citizens
    private String emailAddress;

    private AddressUK address;

    private VerticalYesNo addressKnown;

    private VerticalYesNo addressSameAsProperty;

    private String phoneNumber;

    private VerticalYesNo phoneNumberProvided;

}
