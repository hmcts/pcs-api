package uk.gov.hmcts.reform.pcs.exception;

/**
 * The current user has no organisation (or organisation profile) to create a claim for.
 * */
public class OrganisationNotFoundException extends RuntimeException {

    public OrganisationNotFoundException(String message) {
        super(message);
    }

}
