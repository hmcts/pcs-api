package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import uk.gov.hmcts.reform.pcs.document.model.hearingnotice.HearingNoticePayload;

import java.util.UUID;

/**
 * Everything needed to generate one recipient's notice, loaded inside the read-only transaction so the
 * generation itself runs with no transaction and no lazy JPA access.
 */
public record HearingNoticeRenderContext(UUID partyId, HearingNoticePayload payload, String filename) {
}
