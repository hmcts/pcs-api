package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Flags;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Party {

    private String firstName;

    private String lastName;

    private String orgName;

    private VerticalYesNo nameKnown;

    private String emailAddress;

    private AddressUK address;

    private VerticalYesNo addressKnown;

    private VerticalYesNo addressSameAsProperty;

    private String phoneNumber;

    private VerticalYesNo phoneNumberProvided;

    private LocalDate dateOfBirth;

    @CCD(
        label = "Flags for Appellant",
        retainHiddenValue = true
    )
    private Flags appellantFlags;

    @CCD(
        label = "Flags for Respondent",
        retainHiddenValue = true
    )
    private Flags respondentFlags;

}
