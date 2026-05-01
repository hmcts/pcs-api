package uk.gov.hmcts.reform.pcs.exception;

public class LegalRepresentativeAlreadyLinkedToPartyException extends RuntimeException {

    public LegalRepresentativeAlreadyLinkedToPartyException(String message) {
        super(message);
    }

}
