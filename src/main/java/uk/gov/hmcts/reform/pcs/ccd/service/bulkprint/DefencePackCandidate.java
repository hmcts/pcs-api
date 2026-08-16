package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;

/**
 * One defence-phase envelope to post: typically a postal defendant (claimants are excluded once the new
 * recipient rule is enabled). The role drives address resolution and the documents are the recipient's unsent
 * defence form and/or counter-claim. The coversheet is prepended and the dispatch recorded at send time.
 */
public record DefencePackCandidate(PartyRole role, PartyEntity recipient, List<DocumentEntity> documents) {
}
