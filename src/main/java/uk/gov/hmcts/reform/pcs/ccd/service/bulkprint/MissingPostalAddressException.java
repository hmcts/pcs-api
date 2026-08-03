package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

/**
 * Thrown when a recipient has no usable postal address, so no letter can be sent. Handled per-candidate:
 * logged as a terminal failure and recorded, without aborting the sweep.
 */
public class MissingPostalAddressException extends RuntimeException {

    public MissingPostalAddressException(String message) {
        super(message);
    }
}
