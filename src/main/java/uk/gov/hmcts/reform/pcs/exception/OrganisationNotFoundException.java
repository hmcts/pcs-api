package uk.gov.hmcts.reform.pcs.exception;

/**
 * The current user acts for no organisation that a case can be created for: they hold no organisation
 * role, rd-professional holds no organisation for them, or their organisation has no profile ID.
 */
public class OrganisationNotFoundException extends RuntimeException {

    public OrganisationNotFoundException(String message) {
        super(message);
    }

}
