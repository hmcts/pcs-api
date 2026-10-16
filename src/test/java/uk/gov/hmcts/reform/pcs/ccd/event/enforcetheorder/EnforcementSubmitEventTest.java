package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import com.github.kagkarlsson.scheduler.SchedulerClient;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
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
    private FeeService feeService;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private SchedulerClient schedulerClient;

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
        when(pcsCaseService.loadCase(anyLong())).thenReturn(mock(PcsCaseEntity.class));
        when(partyService.getPrimaryClaimantPartyEntity(any())).thenReturn(mock(PartyEntity.class));

        // When
        SubmitResponse<State> response = event.submit(payload);

        // Then
        verify(enforcementOrderService).saveAndClearDraftData(caseReference, enforcementOrder);
        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo(PENDING_ENFORCEMENT_ISSUED);
        verify(feeService).getFee(any());
        verify(pcsCaseService).loadCase(anyLong());
        verify(partyService).getPrimaryClaimantPartyEntity(any());
        verify(schedulerClient).scheduleIfNotExists(any());
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

}
