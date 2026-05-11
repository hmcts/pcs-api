package uk.gov.hmcts.reform.pcs.ccd.domain.tabs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnderlesseeOrMortgageInformationTabDetails {

    @CCD(
        label = "Underlessee or mortgagee’s address for service known?"
    )
    private String nameKnown;

    @CCD(
        label = "Name"
    )
    private String name;
}
