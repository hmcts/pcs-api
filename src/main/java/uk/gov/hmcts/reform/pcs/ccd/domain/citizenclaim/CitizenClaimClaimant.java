package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimClaimant {

    private CitizenClaimantKind individualOrOrganisation;

    private String firstName;

    private String lastName;

    private String organisationName;

    private AddressUK address;

    private String emailAddress;

    private VerticalYesNo contactByEmail;

    private VerticalYesNo contactByPhone;

    private String phoneNumber;

    private VerticalYesNo contactByText;

    private String textMessageNumber;

    private VerticalYesNo multipleClaimants;

}
