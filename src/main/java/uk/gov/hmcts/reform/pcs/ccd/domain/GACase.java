package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GACase {

    @CCD(label = "Application Id")
    private Long caseReference;

    @CCD(label = "General application type")
    private GAType gaType;

    @CCD(label = "Adjustments")
    private String adjustment;

    @CCD(label = "Parent case")
    private CaseLink caseLink;

    @CCD(label = "Additional information")
    private String additionalInformation;

    @CCD(label = "Status")
    private State status;

}
