package uk.gov.hmcts.reform.pcs.ccd.retention;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.RetainAndDisposePolicy;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import static org.assertj.core.api.Assertions.assertThat;

class RetainAndDisposeContractTest {

    @Test
    void shouldMatchSdkRetainAndDisposeIdentifiers() {
        assertThat(State.PendingDisposal.name()).isEqualTo(RetainAndDisposePolicy.DISPOSAL_STATE_ID);
        assertThat(EventId.MarkForDisposal.name()).isEqualTo(RetainAndDisposePolicy.DISPOSAL_EVENT_ID);
        assertThat(EventId.ConfirmDisposal.name()).isEqualTo(RetainAndDisposePolicy.CONFIRM_DISPOSAL_EVENT_ID);
    }
}
