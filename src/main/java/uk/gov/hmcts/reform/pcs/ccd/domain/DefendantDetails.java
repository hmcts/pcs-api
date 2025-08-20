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
public class DefendantDetails {

    @CCD(label = """
                ---
                <h2>Defendant's Name</h2>""",typeOverride = FieldType.Label)
    private String nameSectionLabel;

    @CCD(label = "Do you know the defendant's name?")
    private VerticalYesNo nameKnown;

    @CCD(label = "Defendant's first name",showCondition = "nameKnown=\"YES\"")
    private String firstName;

    @CCD(label = "Defendant's last name",showCondition = "nameKnown=\"YES\"")
    private String lastName;

    @CCD(label = """
                ---
                <h2>Defendant's correspondence address</h2>""",typeOverride = FieldType.Label)
    private String addressSectionLabel;

    @CCD(label = "Do you know the defendant's correspondence address?")
    private VerticalYesNo addressKnown;

    @CCD(label = "Is the defendant's correspondence address the same as the address of the property"
        + " you're claiming possession of?", showCondition = "addressKnown=\"YES\"")
    private VerticalYesNo addressSameAsPossession;

    @CCD(typeOverride = FieldType.Label,
            label = """
                   <h3 class='govuk-heading-m govuk-!-margin-bottom-1'>Enter address details</h3>
                   <p class='govuk-hint govuk-!-font-size-16 govuk-!-margin-top-1'>If their correspondence address is
                   outside of the UK, you'll need to make a general application for permission to serve a claim
                   outside the jurisdiction after you've submitted and paid for the claim.</p>
                   """,
            showCondition = "addressKnown=\"YES\" AND addressSameAsPossession=\"NO\""
    )
    private String correspondenceAddressHintField;

    @CCD(showCondition = "addressKnown=\"YES\" AND addressSameAsPossession=\"NO\"")
    private AddressUK correspondenceAddress;

    @CCD(label = """
                ---
                <h2>Defendant's email address</h2>""",typeOverride = FieldType.Label)
    private String emailSectionLabel;

    @CCD(label = "Do you know the defendant's email address? (Optional)")
    private VerticalYesNo emailKnown;

    @CCD(label = "Email",  typeOverride = FieldType.Email, showCondition = "emailKnown=\"YES\"")
    private String email;

}

