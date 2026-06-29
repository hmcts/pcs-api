package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;

/**
 * Everything the Docmosis render needs, materialised inside the read-only transaction so the render
 * runs with no transaction and no lazy JPA access — the payload plus the defendant's position used
 * in the filename.
 */
public record CounterClaimFormRenderContext(CounterClaimFormPayload payload, int defendantNumber) {
}
