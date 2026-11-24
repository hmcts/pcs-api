package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.RiskAssessmentClaimantAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.RiskAssessmentInternalAccess;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class EnforcementEventShowConditions {

    @CCD
    private YesOrNo recordWarrantReturn;

    @CCD(searchable = false, access = {RiskAssessmentInternalAccess.class, RiskAssessmentClaimantAccess.class})
    private YesOrNo canViewRiskAssessment;
}
