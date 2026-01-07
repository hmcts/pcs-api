package uk.gov.hmcts.reform.pcs.exception;

import java.util.UUID;

public class EnforcementOrderNotFoundException extends RuntimeException {

    public EnforcementOrderNotFoundException(UUID id) {
        super("No enforcement order found for case reference " + id.toString());
    }
}
