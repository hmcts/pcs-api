package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;

/**
 * The CCD domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {
    @CCD(label = "Description of this case")
    private String caseDescription;

    @External
    private String exampleTabMarkdown;
}
