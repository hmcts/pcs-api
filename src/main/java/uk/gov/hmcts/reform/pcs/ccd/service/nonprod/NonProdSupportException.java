package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

public class NonProdSupportException extends RuntimeException  {

    public NonProdSupportException(Throwable cause) {
        super(cause);
    }

    public NonProdSupportException(String message, Throwable cause) {
        super(message, cause);
    }

}
