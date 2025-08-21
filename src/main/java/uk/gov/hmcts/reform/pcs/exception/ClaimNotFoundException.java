package uk.gov.hmcts.reform.pcs.exception;

import java.util.UUID;

public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(UUID claimId) {
        super("No claim found with ID " + claimId);
    }

}
