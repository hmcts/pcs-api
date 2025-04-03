package uk.gov.hmcts.reform.pcs.exception;

public class PartyNotFoundException extends RuntimeException {

    public PartyNotFoundException(String message) {
        super(message);
    }

}
