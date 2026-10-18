package uk.gov.hmcts.reform.pcs.enforcement.exception;

import java.util.UUID;

public class EnforcementOrderNotFoundException extends RuntimeException {

    public EnforcementOrderNotFoundException(UUID id) {
        super("No enforcement found with ID " + id);
    }
}
