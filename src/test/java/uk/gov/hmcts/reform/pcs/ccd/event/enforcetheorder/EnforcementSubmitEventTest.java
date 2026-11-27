package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_ENFORCEMENT_ISSUED;

@ExtendWith(MockitoExtension.class)
class EnforcementSubmitEventTest {

    private static final String REFUSAL_EXPECTED_STRING = "This action is not available";

    @Mock
    private EnforcementOrderService enforcementOrderService;
    @Mock
    private EventPayload<PCSCase, State> payload;
    @Mock
    private PCSCase caseData;
    @Mock
    private EnforcementFlagGuard enforcementFlagGuard;

    @InjectMocks
    private EnforcementSubmitEvent event;

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT", "WRIT"})
    void submit_savesEnforcementOrderAndReturnsDefaultResponse(SelectEnforcementType enforcementType) {
        // Given
        long caseReference = 123456789L;
        String feeAmount = "122.21";
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
                .value(DynamicStringListElement.builder().code(enforcementType.name()).build()).build())
                .warrantFeeAmount(feeAmount)
                .writFeeAmount(feeAmount)
            .build();

        when(payload.caseReference()).thenReturn(caseReference);
        when(payload.caseData()).thenReturn(caseData);
        when(caseData.getEnforcementOrder()).thenReturn(enforcementOrder);

        // When
        SubmitResponse<State> response = event.submit(payload);

        // Then
        verify(enforcementOrderService).saveAndClearDraftData(caseReference, enforcementOrder);
        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo(PENDING_ENFORCEMENT_ISSUED);
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class,
        names = {"WARRANT_OF_RESTITUTION", "WRIT_OF_RESTITUTION"})
    void submit_savesEnforcementOrderAndReturnsDefaultResponse_Not_Implemented(SelectEnforcementType enforcementType) {
        // Given
        long caseReference = 123456789L;
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
               .value(DynamicStringListElement.builder().code(enforcementType.name()).build())
               .build())
            .build();

        when(payload.caseReference()).thenReturn(caseReference);
        when(payload.caseData()).thenReturn(caseData);
        when(caseData.getEnforcementOrder()).thenReturn(enforcementOrder);

        // When // Then
        assertThatThrownBy(() -> event.submit(payload))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unimplemented enforcement type: " + enforcementType);
    }

    @Test
    void givenEnforcementFlagOff_whenSubmit_thenReturnRefusalError() {

        when(enforcementFlagGuard.isOff()).thenReturn(true);
        when(enforcementFlagGuard.refused()).thenCallRealMethod();
        List<String> expectedErrors = List.of(REFUSAL_EXPECTED_STRING);

        SubmitResponse<State> actualResponse = event.submit(payload);

        assertThat(actualResponse.getErrors()).isEqualTo(expectedErrors);
    }

    @Test
    void givenEnforcementFlagOff_whenSubmit_thenDoNotCallEnforcementOrderService() {

        when(enforcementFlagGuard.isOff()).thenReturn(true);
        when(enforcementFlagGuard.refused()).thenCallRealMethod();


        event.submit(payload);

        verifyNoInteractions(enforcementOrderService);
    }

    @Test
    void givenEnforcementFlagOff_whenSubmit_thenLeaveStateUnchanged() {
        when(enforcementFlagGuard.isOff()).thenReturn(true);
        when(enforcementFlagGuard.refused()).thenCallRealMethod();

        SubmitResponse<State> actualResponse = event.submit(payload);

        assertThat(actualResponse.getState()).isNull();
    }

}
