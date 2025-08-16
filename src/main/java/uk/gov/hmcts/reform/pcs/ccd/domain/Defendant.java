package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

@Builder
@Data
public class Defendant {

    @CCD(label = """
                ---
                <h2>Defendant's Name</h2>""",typeOverride = FieldType.Label)
    private String nameSectionLabel;

    @CCD(label = "Do you know the defendant's name?")
    private VerticalYesNo defendantsNameKnown;

    @CCD(label = "Defendant's first name",showCondition = "defendantsNameKnown=\"YES\"")
    private String firstName;

    @CCD(label = "Defendant's last name",showCondition = "defendantsNameKnown=\"YES\"")
    private String lastName;

    @CCD(label = """
                ---
                <h2>Defendant's correspondence address</h2>""",typeOverride = FieldType.Label)
    private String addressSectionLabel;

    @CCD(label = "Do you know the defendant's correspondence address?")
    private VerticalYesNo defendantsAddressKnown;

    @CCD(label = "Is the defendant's correspondence address the same as the address of the property"
        + " you're claiming possession of?", showCondition = "defendantsAddressKnown=\"YES\"")
    private VerticalYesNo defendantsAddressSameAsPossession;


    @CCD(label = "Enter address details", showCondition = "defendantsAddressKnown=\"YES\"" +
            " AND defendantsAddressSameAsPossession=\"NO\"")
    private AddressUK correspondenceAddress;

    @CCD(label = """
                ---
                <h2>Defendant's email address</h2>""",typeOverride = FieldType.Label)
    private String emailSectionLabel;

    @CCD(label = "Do you know the defendant's email address?(optional)")
    private VerticalYesNo defendantsEmailKnown;

    @CCD(label = "Email", showCondition = "defendantsEmailKnown=\"YES\"")
    private String email;

}

