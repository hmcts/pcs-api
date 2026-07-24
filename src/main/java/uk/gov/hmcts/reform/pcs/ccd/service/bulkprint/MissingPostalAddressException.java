package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactedRuntimeException;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

/**
 * Thrown when a recipient has no usable postal address, so no letter can be sent. Handled per-candidate:
 * logged as a terminal failure and recorded, without aborting the sweep.
 */
public class MissingPostalAddressException extends RedactedRuntimeException {

    public MissingPostalAddressException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }
}
