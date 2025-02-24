package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Data
public class Party {
    @CCD(label = "Party's forename")
    private String forename;
    @CCD(label = "Party's surname")
    private String surname;
}
