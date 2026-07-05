package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;

/**
 * One defence-phase envelope to post: the recipient (claimant or a defendant), their role (drives address
 * resolution), and the unsent defence-side documents for them (defence form and/or counter-claim). Recorded
 * in one {@code PACK_SENT} row for the dispatch; the coversheet is prepended at send time.
 */
public record DefencePackCandidate(PartyRole role, PartyEntity recipient, List<DocumentEntity> documents) {
}
