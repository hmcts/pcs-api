package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;

/**
 * One envelope to post: the recipient and the ordered pack documents. The coversheet is prepended at send time.
 * A bilingual pack carries the Welsh access-code letter and is posted with the Welsh blank forms. A defendant's
 * pack on a citizen claim is also posted with the HLPAS legal advice leaflet.
 */
public record ClaimPackCandidate(PartyRole recipientType, PartyEntity party, List<DocumentEntity> documents,
                                 boolean bilingual, boolean includesHlpasLeaflet) {

    public ClaimPackCandidate(PartyRole recipientType, PartyEntity party, List<DocumentEntity> documents) {
        this(recipientType, party, documents, false, false);
    }

    public ClaimPackCandidate(PartyRole recipientType, PartyEntity party, List<DocumentEntity> documents,
                              boolean bilingual) {
        this(recipientType, party, documents, bilingual, false);
    }
}
