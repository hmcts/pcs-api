package uk.gov.hmcts.reform.pcs.exception;

import java.util.UUID;

public class CaseNotFoundException extends RuntimeException {

    public CaseNotFoundException(UUID pcsCaseId) {
        super("No case found with internal ID " + pcsCaseId);
    }

    public CaseNotFoundException(long caseReference) {
        super("No case found with reference " + caseReference);
    }

}
