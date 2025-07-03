package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GACase {

    @CCD(label = "Application Id", access = CaseworkerAccess.class)
    private Long caseReference;

    @CCD(label = "General application type", access = CaseworkerAccess.class)
    private GAType gaType;

    @CCD(label = "Adjustments", access = CaseworkerAccess.class)
    private String adjustment;

    @CCD(label = "Parent case", access = CaseworkerAccess.class)
    private CaseLink caseLink;

    @CCD(label = "Additional information", access = CaseworkerAccess.class)
    private String additionalInformation;

    @CCD(label = "Status", access = CaseworkerAccess.class)
    private State status;

}
