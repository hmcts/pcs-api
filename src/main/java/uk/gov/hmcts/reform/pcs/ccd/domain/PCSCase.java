package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

/**
 * The CCD domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {
    @CCD(label = "Description of this case")
    private String caseDescription;

    // Markdown string making up the 'example' tab in XUI.
    // Set dynamically upon case load
    private String exampleTabMarkdown;

    @CCD(label = "Party first name")
    private String partyFirstName;
    @CCD(label = "Party last name")
    private String partyLastName;
}
