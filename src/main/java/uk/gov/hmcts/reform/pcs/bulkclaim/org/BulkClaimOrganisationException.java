package uk.gov.hmcts.reform.pcs.bulkclaim.org;

import lombok.Getter;

@Getter
public class BulkClaimOrganisationException extends RuntimeException {

    public enum Reason {
        NOT_FOUND,
        NOT_ACTIVE,
        PROFILE_NOT_USABLE,
        NOT_A_SOLICITOR_ORGANISATION
    }

    private final Reason reason;

    public BulkClaimOrganisationException(Reason reason) {
        super("Organisation cannot be used for bulk claims: " + reason);
        this.reason = reason;
    }
}
