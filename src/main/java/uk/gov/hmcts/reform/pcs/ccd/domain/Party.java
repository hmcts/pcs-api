package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Party {

    private String firstName;

    private String lastName;

    private String orgName;

    private SimpleYesNo nameKnown;

    private String emailAddress;

    private AddressUK address;

    private SimpleYesNo addressKnown;

    private SimpleYesNo addressSameAsProperty;

    private String phoneNumber;

    private SimpleYesNo phoneNumberProvided;

    private LocalDate dateOfBirth;

}
