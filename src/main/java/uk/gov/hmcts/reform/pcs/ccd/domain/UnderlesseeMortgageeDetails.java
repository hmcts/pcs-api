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
    private String underlesseeOrMortgageeNameLabel;

    @CCD(label = "Do you know the underlessee or mortgagee's name?")
    private VerticalYesNo underlesseeOrMortgageeNameKnown;

    @CCD(label = "What is their name?",
        hint = "Enter their first and last name, or the company or organisation name",
        showCondition = "underlesseeOrMortgageeNameKnown=\"YES\""
    )
    private String underlesseeOrMortgageeName;

    @CCD(label = """
                    ---
                    <h2 class="govuk-heading-m">Underlessee or mortgagee correspondence address</h2>
                    """, typeOverride = FieldType.Label)
    private String underlesseeOrMortgageeAddressLabel;

    @CCD(label = "Do you know the underlessee or mortgagee's correspondence address?")
    private VerticalYesNo underlesseeOrMortgageeAddressKnown;

    @CCD(label = "Enter address details", showCondition = "underlesseeOrMortgageeAddressKnown=\"YES\"")
    private AddressUK underlesseeOrMortgageeAddress;

}
