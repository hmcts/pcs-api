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
public class UnderlesseeMortgageeDetails {

    @CCD(label = """
                    ---
                    <h2 class="govuk-heading-m"> Underlessee or mortgagee name </h2>
                    """, typeOverride = FieldType.Label)
    private String nameSectionLabel;

    @CCD(label = "Do you know the underlessee or mortgagee’s name?")
    private VerticalYesNo nameKnown;

    @CCD(label = "What is their name?",
        hint = "Enter their first and last name, or the company or organisation name",
        showCondition = "nameKnown=\"YES\""
    )
    private String name;

    @CCD(label = """
                    ---
                    <h2 class="govuk-heading-m">Underlessee or mortgagee correspondence address</h2>
                    """, typeOverride = FieldType.Label)
    private String addressSectionLabel;

    @CCD(label = "Do you know the underlessee or mortgagee’s correspondence address?")
    private VerticalYesNo addressKnown;

    @CCD(label = "Enter address details", showCondition = "addressKnown=\"YES\"")
    private AddressUK address;

}
