package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralApplication {

    @CCD(ignore = true)
    @JsonIgnore
    private UUID id;

    @CCD(label = "Adjustments")
    private String adjustment;

    @CCD(label = "Additional information")
    private String additionalInformation;

    @CCD(label = "status")
    private State status;


}
