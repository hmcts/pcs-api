package uk.gov.hmcts.reform.pcs.exception;

import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

public class DraftNotFoundException extends RedactedRuntimeException {

    public DraftNotFoundException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }
}
