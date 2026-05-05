package uk.gov.hmcts.reform.pcs.ccd.event.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitDashboardViewHandlerTest {

    private static final long CASE_REFERENCE = 555L;

    @Mock
    private EventPayload<PCSCase, State> eventPayload;

    private SubmitDashboardViewHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new SubmitDashboardViewHandler();
    }

    @Test
    void shouldReturnDefaultSubmitResponseWithoutErrors() {
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);

        SubmitResponse<State> result = underTest.submit(eventPayload);

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();
        assertThat(result.getState()).isNull();
    }
}
