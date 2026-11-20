package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

public class CitizenClaimPaymentUnavailableException extends RuntimeException {

    public CitizenClaimPaymentUnavailableException(long caseReference, Throwable cause) {
        super("Unable to create the claim fee service request for case " + caseReference, cause);
    }

}
