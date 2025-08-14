package uk.gov.hmcts.reform.pcs.ccd.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
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
public class Defendant {

    @CCD(label = """
                ---
                <h2>Defendant's Name</h2>""",typeOverride = FieldType.Label)
    private String nameSectionLabel;

    @CCD(label = "Do you know the defendant's name?")
    private VerticalYesNo defendantsNameKnown;

    @CCD(label = "Defendant's first name")
    private String firstName;

    @CCD(label = "Defendant's last name")
    private String lastName;

    @CCD(label = """
                ---
                <h2>Defendant's correspondence address</h2>""",typeOverride = FieldType.Label)
    private String addressSectionLabel;

    @CCD(label = "Do you know the defendant's correspondence address?")
    private VerticalYesNo defendantsAddressKnown;

    @CCD(label = "Is the defendant's correspondence address the same as the address of the property"
        + " you're claiming possession of?")
    private VerticalYesNo defendantsAddressSameAsPossession;

    @JsonProperty("correspondenceAddress")
    @CCD(label = "Enter a UK Address")
    private AddressUK correspondenceAddress;

    @CCD(label = """
                ---
                <h2>Defendant's Email</h2>""",typeOverride = FieldType.Label)
    private String emailSectionLabel;

    @CCD(label = "Do you know the defendant's email?")
    private VerticalYesNo defendantsEmailKnown;

    @CCD(label = "Email")
    private String email;
}
