package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

import uk.gov.hmcts.reform.pcs.document.model.warrantofpossession.WarrantOfPossessionPayload;

public record WarrantOfPossessionRenderContext(WarrantOfPossessionPayload payload, Integer rank) {
}
