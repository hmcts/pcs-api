package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import uk.gov.hmcts.reform.pcs.document.model.defenceform.DefenceFormPayload;

/**
 * Everything the Docmosis render needs, materialised inside the read-only transaction so the render
 * itself runs with no transaction and no lazy JPA access — the payload plus the responding
 * defendant's position used in the filename.
 */
public record DefenceFormRenderContext(DefenceFormPayload payload, int defendantNumber) {
}
