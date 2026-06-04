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
public class LegalRepresentativeDetails {

    @CCD(label = "Do you want to use this email address for notifications?")
    private VerticalYesNo useEmailAddress;

    @CCD(
        label = "Defendant’s legal representative's email address",
        typeOverride = FieldType.Email
    )
    private String emailAddress;

    @CCD(typeOverride = FieldType.Email)
    private String originalEmailAddress;

    @CCD(
        label = "Defendant’s legal representative's reference",
        max = 60
    )
    private String reference;

    @CCD(label = "Do you want to provide a contact phone number?")
    private VerticalYesNo provideContactPhoneNumber;

    @CCD(label = "Enter phone number",
        regex = "^(?:0[12](?:\\s*\\d){8,9}|0[3789](?:\\s*\\d){9})$",
        max = 40)
    private String contactPhoneNumber;

    @CCD(label = """
                ---
                <h2>Defendant’s name</h2>""", typeOverride = FieldType.Label)
    private String nameSectionLabel;


    @CCD(label = "Do you want to enter a different postal address?")
    private VerticalYesNo differentPostalAddress;

    @CCD(label = "Enter address details")
    private AddressUK updatedCorrespondenceAddress;

    @CCD
    private AddressUK legalRepresentativeOrganisationAddress;

    private YesOrNo organisationAddressFound;

    private String formattedContactAddress;

}

