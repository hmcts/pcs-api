package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenancyTabDetails {

    @CCD(label = "Tenancy, occupation contract or licence agreement type")
    private String agreementType;

    @CCD(label = "Tenancy, occupation contract or licence agreement start date")
    private String agreementStartDate;
}
