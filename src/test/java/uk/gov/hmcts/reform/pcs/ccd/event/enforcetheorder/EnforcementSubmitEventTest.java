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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_ENFORCEMENT_ISSUED;

@ExtendWith(MockitoExtension.class)
class EnforcementSubmitEventTest {

    @Mock
    private EnforcementOrderService enforcementOrderService;
    @Mock
    private EventPayload<PCSCase, State> payload;
    @Mock
    private PCSCase caseData;
    @Mock
    private MoneyFormatter moneyFormatter;

    @InjectMocks
    private EnforcementSubmitEvent event;

    @Test
    void submit_savesEnforcementOrderAndReturnsDefaultResponse() {
        // Given
        long caseReference = 123456789L;
        String feeAmount = "122.21";
        EnforcementOrder enforcementOrder = new EnforcementOrder();
        enforcementOrder.setSelectEnforcementType(SelectEnforcementType.WARRANT);
        enforcementOrder.setWarrantFeeAmount(feeAmount);
        when(payload.caseReference()).thenReturn(caseReference);
        when(payload.caseData()).thenReturn(caseData);
        when(caseData.getEnforcementOrder()).thenReturn(enforcementOrder);
        when(moneyFormatter.formatFee(any(BigDecimal.class))).thenReturn(feeAmount);

        // When
        SubmitResponse<State> response = event.submit(payload);

        // Then
        verify(enforcementOrderService).saveAndClearDraftData(caseReference, enforcementOrder);
        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo(PENDING_ENFORCEMENT_ISSUED);
    }

}
