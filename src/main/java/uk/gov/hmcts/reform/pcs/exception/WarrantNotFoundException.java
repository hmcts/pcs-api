package uk.gov.hmcts.reform.pcs.exception;

public class WarrantNotFoundException extends RuntimeException {

    public WarrantNotFoundException(String message) {
        super(message);
    }

}
