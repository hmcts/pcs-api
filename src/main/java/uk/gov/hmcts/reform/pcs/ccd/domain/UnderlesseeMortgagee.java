package uk.gov.hmcts.reform.pcs.ccd.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnderlesseeMortgagee {

    @CCD(label = "Is there an underlessee or mortgagee entitled to claim relief against forfeiture?")
    private VerticalYesNo hasUnderlesseeOrMortgagee;

    @CCD(label = "Do you know the underlessee or mortgagee's name?")
    private VerticalYesNo underlesseeOrMortgageeNameKnown;

    @CCD(label = "Do you know the underlessee or mortgagee's correspondence address?")
    private VerticalYesNo underlesseeOrMortgageeAddressKnown;

    @CCD(label = "Do you need to add another underlessee or mortgagee?")
    private VerticalYesNo addAdditionalUnderlesseeOrMortgagee;

}
