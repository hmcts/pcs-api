package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Data
public class PCSCase {
    @CCD(label = "A field")
    private String applicantForename;
}
