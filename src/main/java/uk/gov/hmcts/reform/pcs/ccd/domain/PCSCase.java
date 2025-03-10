package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {
    @CCD(label = "Description of this case")
    private String caseDescription;

    @CCD(label = "Party A")
    private Party partyA;

    @External
    private String exampleTabMarkdown;
}
