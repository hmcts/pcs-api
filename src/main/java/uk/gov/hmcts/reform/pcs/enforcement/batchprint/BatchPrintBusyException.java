package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

public class BatchPrintBusyException extends RuntimeException {

    public static final String MESSAGE =
        "Too many batch prints are being produced at the moment. Try printing these cases again in a few minutes";

    public BatchPrintBusyException() {
        super(MESSAGE);
    }
}
