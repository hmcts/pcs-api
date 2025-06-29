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
public class GACase {

    @CCD(label = "Case reference")
    private Long caseReference;

    @CCD(label = "Adjustments")
    private String adjustment;

    @CCD(label = "Additional information")
    private String additionalInformation;

    @CCD(label = "status")
    private State status;

}
