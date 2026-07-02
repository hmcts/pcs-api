package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;

/**
 * One defence-pack envelope to post to a defendant, plus the pack status it completes to:
 * {@code DEFENCE_PACK_PARTIALLY_SENT} when a counter-claim is still outstanding, else {@code DEFENCE_PACK_SENT}.
 */
public record DefencePackCandidate(PartyEntity defendant, List<DocumentEntity> documents,
                                   ClaimActivityType targetStatus) {
}
