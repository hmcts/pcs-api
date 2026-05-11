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
public class SuspensionOfRightToBuyTabDetails {

    @CCD(
        label = "Section of the Housing Act suspension of right to buy claim made under"
    )
    private String section;

    @CCD(
        label = "Reasons for requesting suspension of right to buy order"
    )
    private String reasons;
}
