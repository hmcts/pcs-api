package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@AllArgsConstructor
public class GeneralApplication {

    @CCD(ignore = true)
    @JsonIgnore
    private Long parentCaseReference;

    @CCD(ignore = true)
    @JsonIgnore
    private Long applicationId;


    @CCD(label = "Adjustments")
    private String adjustment;

    @CCD(label = "status")
    private State status;


}
