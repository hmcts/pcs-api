package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

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
public class AdditionalDefendantInformationTabDetails {
    @CCD(
        label = "Additional defendant"
    )
    private DefendantInformationTabDetails defendantInformationDetails;

    @CCD(
        label = "Additional defendant address for service"
    )
    private AddressUK defendantOneAddress;
}
