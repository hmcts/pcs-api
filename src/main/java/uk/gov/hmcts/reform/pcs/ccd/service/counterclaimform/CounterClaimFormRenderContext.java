package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;

public record CounterClaimFormRenderContext(long caseReference, CounterClaimFormPayload payload, int defendantNumber) {
}
