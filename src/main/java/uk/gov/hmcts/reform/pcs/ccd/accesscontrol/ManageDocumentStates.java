package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public final class ManageDocumentStates {

    public static final State[] MANAGE_DOCUMENT_STATES = {
        State.PENDING_CASE_ISSUED,
        State.CASE_ISSUED
    };

    private ManageDocumentStates() {
    }
}