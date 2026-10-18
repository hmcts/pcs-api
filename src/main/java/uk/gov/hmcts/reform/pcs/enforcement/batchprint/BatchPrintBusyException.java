package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

/**
 * The instance was already building as many batch prints as it will fetch documents for, and this request
 * waited its turn for as long as {@link BatchPrintService#MAX_WAIT_FOR_A_SLOT} allows.
 *
 * <p>Refusing the whole request is deliberate: waiting any longer holds a request thread past the gateway's
 * own timeout, and leaving the cases that did not get a slot out of the file would give the bailiff a batch
 * print that silently omits cases the court expects them to evict.
 */
public class BatchPrintBusyException extends RuntimeException {

    public static final String MESSAGE =
        "Too many batch prints are being produced at the moment. Try printing these cases again in a few minutes";

    public BatchPrintBusyException() {
        super(MESSAGE);
    }
}
