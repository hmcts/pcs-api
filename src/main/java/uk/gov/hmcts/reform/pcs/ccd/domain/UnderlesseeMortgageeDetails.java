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

    @CCD(label = "Is the underlessee or mortgagee an individual or an organisation?",
        showCondition = "nameKnown=\"YES\""
    )
    private IndividualOrOrganisation partyType;

    @CCD(label = "First name",
        showCondition = "nameKnown=\"YES\" AND partyType=\"INDIVIDUAL\"",
        max = 60
    )
    private String firstName;

    @CCD(label = "Last name",
        showCondition = "nameKnown=\"YES\" AND partyType=\"INDIVIDUAL\"",
        max = 60
    )
    private String lastName;

    @CCD(label = "What is the organisation’s name?",
        hint = "Enter the company or organisation name",
        showCondition = "nameKnown=\"YES\" AND partyType=\"ORGANISATION\"",
        max = 60
    )
    private String organisationName;

    @CCD(label = """
                    ---
                    <h2 class="govuk-heading-m">Underlessee or mortgagee address for service</h2>
                    """, typeOverride = FieldType.Label)
    private String addressSectionLabel;

    @CCD(label = "Do you know the underlessee or mortgagee’s address for service?")
    private VerticalYesNo addressKnown;

    @CCD(label = "Enter address details", showCondition = "addressKnown=\"YES\"")
    private AddressUK address;

}
