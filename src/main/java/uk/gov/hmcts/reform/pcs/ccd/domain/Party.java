package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.InternalCaseFlagAccess;

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
        access = {InternalCaseFlagAccess.class},
        label = "Party Flags",
        retainHiddenValue = true
    )
    private Flags respondentFlags;

}
