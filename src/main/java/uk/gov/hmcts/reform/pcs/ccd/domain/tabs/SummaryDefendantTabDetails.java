package uk.gov.hmcts.reform.pcs.ccd.domain.tabs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummaryDefendantTabDetails {

    @CCD(
        label = "First name"
    )
    private String firstName;

    @CCD(
        label = "Last name"
    )
    private String lastName;

}
