package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementSubmitEventTest {

    @Mock
    private EnforcementOrderService enforcementOrderService;
    @Mock
    private EventPayload<PCSCase, State> payload;
    @Mock
    private PCSCase caseData;

    @InjectMocks
    private EnforcementSubmitEvent event;

    @Test
    void submit_savesEnforcementOrderAndReturnsDefaultResponse() {
        // Given
        long caseReference = 123456789L;
        EnforcementOrder enforcementOrder = new EnforcementOrder();
        when(payload.caseReference()).thenReturn(caseReference);
        when(payload.caseData()).thenReturn(caseData);
        when(caseData.getEnforcementOrder()).thenReturn(enforcementOrder);

        // When
        SubmitResponse<State> response = event.submit(payload);

        // Then
        verify(enforcementOrderService).saveAndClearDraftData(caseReference, enforcementOrder);
        assertThat(response).isNotNull();
        assertThat(SubmitResponse.<State>defaultResponse()).isEqualTo(response);
    }
}
