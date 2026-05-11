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
public class ApplicationsTabDetails {

    @CCD(
        label = "Are you planning to make an application at the same time as your claim?"
    )
    private String planToMakeGeneralApplication;
}
