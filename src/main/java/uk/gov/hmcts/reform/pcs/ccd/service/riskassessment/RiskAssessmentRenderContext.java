package uk.gov.hmcts.reform.pcs.ccd.service.riskassessment;

import uk.gov.hmcts.reform.pcs.document.model.riskassessment.RiskAssessmentPayload;

public record RiskAssessmentRenderContext(RiskAssessmentPayload payload, Integer rank) {
}
