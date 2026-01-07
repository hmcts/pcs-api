package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimantContactPreferences {

    @CCD
    private YesOrNo orgAddressFound;

    @CCD(typeOverride = FieldType.Email)
    private String claimantContactEmail;

    @CCD(label = "Do you want to use this email address for notifications?")
    private VerticalYesNo isCorrectClaimantContactEmail;

    @CCD(label = "Enter email address", typeOverride = FieldType.Email)
    private String overriddenClaimantContactEmail;

    private String formattedClaimantContactAddress;

    @CCD(label = "Do you want documents to be sent to this address?")
    private VerticalYesNo isCorrectClaimantContactAddress;

    @CCD(label = "Enter address details")
    private AddressUK overriddenClaimantContactAddress;

    @CCD
    private AddressUK missingClaimantAddress;

    @CCD(label = "Do you want to provide a contact phone number?")
    private VerticalYesNo claimantProvidePhoneNumber;

    @CCD(label = "Enter phone number", regex = "^\\s*0\\d{10}\\s*$")
    private String claimantContactPhoneNumber;

}

