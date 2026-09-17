package uk.gov.hmcts.reform.pcs.exception;

import java.util.UUID;

public class DraftResponseDataDeletionException extends RuntimeException {

    public DraftResponseDataDeletionException(long caseReference, String event, UUID partyId, String organisationId) {
        super("Failed to delete draft response data for case reference: " + caseReference + ", event: " + event
                + ", party: " + partyId + ", organisation: " + organisationId);
    }
}
