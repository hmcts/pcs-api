package uk.gov.hmcts.reform.pcs.exception;

import java.util.UUID;

public class GenAppNotFoundException extends RuntimeException {

    public GenAppNotFoundException(UUID claimId) {
        super("No general application found with ID " + claimId);
    }

}
