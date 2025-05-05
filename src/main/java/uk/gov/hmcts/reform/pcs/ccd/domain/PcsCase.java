package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

/**
 * The CCD domain model representing a possessions case.
 */
@Builder
@Data
public class PcsCase {

    @CCD(label = "Case reference")
    private String hyphenatedCaseRef;

    @CCD(label = "Description of this case")
    private String caseDescription;

    private String roleMarkdown;

    @CCD(label = "Applicant name")
    private String applicantName;

    @CCD(label = "Applicant address", access = ApplicantAccess.class)
    private AddressUK applicantAddress;

    @CCD(label = "Respondent name")
    private String respondentName;

    @CCD(label = "Respondent address")
    private AddressUK respondentAddress;

    private String caseSummaryMarkdown;

    private String eventsMarkdown;

    private String detailsForApplicantMarkdown;

    private String detailsForRespondentMarkdown;

}
