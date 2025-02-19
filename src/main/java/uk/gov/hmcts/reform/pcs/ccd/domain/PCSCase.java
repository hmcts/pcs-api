package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

@Data
public class PCSCase {
    @CCD(label = "Applicant's first name")
    private String applicantForename;

    private List<ListValue<Party>> parties;
}
