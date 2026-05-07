package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalRepresentativeDetails {

    @CCD(
        label = "Defendant’s legal representative's email address",
        typeOverride = FieldType.Email
    )
    private String emailAddress;

    @CCD(
        label = "Defendant’s legal representative's reference",
        max = 60
    )
    private String reference;

    @CCD(label = "Do you want to provide a contact phone number?")
    private VerticalYesNo provideContactPhoneNumber;

    @CCD(label = "Enter phone number", regex = "^\\s*0\\d{10}\\s*$", max = 60, showCondition = "provideContactPhoneNumber=\"YES\"")
    private String contactPhoneNumber;

    @CCD(label = """
                ---
                <h2>Defendant’s name</h2>""", typeOverride = FieldType.Label)
    private String nameSectionLabel;


    @CCD(label = "Do you want to enter a different postal address?")
    private VerticalYesNo differentPostalAddress;

    @CCD(label = "Enter address details", showCondition = "differentPostalAddress=\"YES\"")
    private AddressUK correspondenceAddress;

}

