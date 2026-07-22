package uk.gov.hmcts.reform.pcs.exception;

public class HearingNotFoundException extends RuntimeException {

    public HearingNotFoundException(String message) {
        super(message);
    }

}
