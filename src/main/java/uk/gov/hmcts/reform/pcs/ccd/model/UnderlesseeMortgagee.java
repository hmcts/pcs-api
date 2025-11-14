package uk.gov.hmcts.reform.pcs.ccd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnderlesseeMortgagee {

    private Boolean underlesseeOrMortgageeNameKnown;

    private String underlesseeOrMortgageeName;

    private Boolean underlesseeOrMortgageeAddressKnown;

    private AddressUK underlesseeOrMortgageeAddress;

    private Boolean addAdditionalUnderlesseeOrMortgagee;
}
