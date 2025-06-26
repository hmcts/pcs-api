package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralApplication {

    @CCD(label = "Application ID")
    private String applicationId;

    @CCD(label = "Adjustments")
    private String adjustment;

    @CCD(label = "Additional information")
    private String additionalInformation;

    @CCD(label = "status")
    private State status;


}
