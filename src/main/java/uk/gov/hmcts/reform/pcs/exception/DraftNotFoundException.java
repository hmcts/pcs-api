package uk.gov.hmcts.reform.pcs.exception;

import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

public class DraftNotFoundException extends RuntimeException {

    public DraftNotFoundException(long caseReference, EventId eventId) {
        super(String.format("No draft found for this case reference %s, eventId %s, and user ",
            caseReference, eventId));
    }
}
