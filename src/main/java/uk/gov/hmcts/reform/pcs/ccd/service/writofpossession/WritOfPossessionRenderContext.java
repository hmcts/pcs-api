package uk.gov.hmcts.reform.pcs.ccd.service.writofpossession;

import uk.gov.hmcts.reform.pcs.document.model.writofpossession.WritOfPossessionPayload;

public record WritOfPossessionRenderContext(WritOfPossessionPayload payload, Integer rank) {
}
