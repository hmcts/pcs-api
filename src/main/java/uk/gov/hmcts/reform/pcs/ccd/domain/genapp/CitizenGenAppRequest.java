package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenGenAppRequest {

    private GenAppType applicationType;

    private VerticalYesNo within14Days;

    private VerticalYesNo needHwf;

    private VerticalYesNo appliedForHwf;

    @CCD(max = 16)
    private String hwfReference;

}
