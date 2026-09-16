package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;

import java.util.Optional;

/**
 * Whether a defence-phase form came from a response submitted by the defendant's legal representative, judged by
 * who completed the statement of truth. A counter-claim form is judged by its defendant's response.
 */
final class LegalRepResponseDocuments {

    private LegalRepResponseDocuments() {
    }

    static boolean isFromLegalRepResponse(DocumentEntity document) {
        Optional<DefendantResponseEntity> response = document.getCounterClaim() != null
            ? document.getCounterClaim().findAssociatedDefendantResponse()
            : Optional.ofNullable(document.getDefendantResponse());
        return response
            .map(DefendantResponseEntity::getStatementOfTruth)
            .map(StatementOfTruthEntity::isCompletedByLegalRepresentative)
            .orElse(false);
    }
}
