package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ManageDocumentStates.MANAGE_DOCUMENT_STATES;

class ManageDocumentStatesTest {

    @Test
    void shouldContainAllDocumentManagementStates() {
        assertThat(MANAGE_DOCUMENT_STATES).containsExactlyInAnyOrder(
            State.CASE_ISSUED,
            State.CASE_PROGRESSION,
            State.CASE_STAYED,
            State.BREATHING_SPACE,
            State.JUDICIAL_REFERRAL,
            State.HEARING_READINESS,
            State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
            State.DECISION_OUTCOME,
            State.ALL_FINAL_ORDERS_ISSUED,
            State.CLOSED
        );
    }
}
