package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;

/**
 * One envelope to post: the recipient and the ordered pack documents. The coversheet is prepended at send time.
 */
public record PackCandidate(PartyRole recipientType, PartyEntity party, List<DocumentEntity> documents) {
}
