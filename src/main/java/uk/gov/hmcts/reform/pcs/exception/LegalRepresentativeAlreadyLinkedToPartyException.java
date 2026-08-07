package uk.gov.hmcts.reform.pcs.exception;

public class LegalRepresentativeAlreadyLinkedToPartyException extends RedactedRuntimeException {

    public LegalRepresentativeAlreadyLinkedToPartyException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }

}
