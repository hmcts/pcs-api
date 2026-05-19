package uk.gov.hmcts.reform.pcs.noc.exception;

import lombok.Getter;

@Getter
public class NocException extends RuntimeException {

    private final String code;

    public NocException(String code, String message) {
        super(message);
        this.code = code;
    }
}
