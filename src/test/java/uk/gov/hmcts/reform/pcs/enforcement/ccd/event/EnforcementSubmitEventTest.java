package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

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
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.exception.LiveEnforcementExistsException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_ENFORCEMENT_ISSUED;

@ExtendWith(MockitoExtension.class)
class EnforcementSubmitEventTest {

    private static final long CASE_REFERENCE = 123456789L;
    private static final String FEE_AMOUNT = "122.21";

    @Mock
    private EnforcementOrderService enforcementOrderService;

    @InjectMocks
    private EnforcementSubmitEvent event;

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT", "WRIT"})
    void submit_savesEnforcementOrderAndAsksForTheFee(SelectEnforcementType enforcementType) {
        EnforcementOrder enforcementOrder = enforcementOrder(enforcementType);
        enforcementOrder.setWarrantFeeAmount(FEE_AMOUNT);
        enforcementOrder.setWritFeeAmount(FEE_AMOUNT);

        SubmitResponse<State> response = event.submit(payload(enforcementOrder));

        verify(enforcementOrderService).saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getState()).isEqualTo(PENDING_ENFORCEMENT_ISSUED);
        assertThat(response.getConfirmationBody())
            .contains("Pay %s for HM Courts and Tribunals Service".formatted(FEE_AMOUNT))
            .contains("/cases/case-details/%d#Service%%20Request".formatted(CASE_REFERENCE));
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT_OF_RESTITUTION", "WRIT_OF_RESTITUTION"})
    void submit_savesARestitutionApplicationWithoutAPaymentPanel(SelectEnforcementType enforcementType) {
        EnforcementOrder enforcementOrder = enforcementOrder(enforcementType);

        SubmitResponse<State> response = event.submit(payload(enforcementOrder));

        verify(enforcementOrderService).saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getState()).isEqualTo(PENDING_ENFORCEMENT_ISSUED);
        assertThat(response.getConfirmationBody()).isNull();
    }

    @Test
    void submit_doesNotAskForAFeeWhenTheFeeLookupFoundNone() {
        EnforcementOrder enforcementOrder = enforcementOrder(SelectEnforcementType.WARRANT);

        SubmitResponse<State> response = event.submit(payload(enforcementOrder));

        assertThat(response.getState()).isEqualTo(PENDING_ENFORCEMENT_ISSUED);
        assertThat(response.getConfirmationBody()).isNull();
    }

    @Test
    void submit_refusesAnApplicationWithNoEnforcementTypeChosen() {
        SubmitResponse<State> response = event.submit(payload(EnforcementOrder.builder().build()));

        assertThat(response.getErrors()).containsExactly(EnforcementSubmitEvent.NO_ENFORCEMENT_TYPE_ERROR);
        assertThat(response.getState()).isNull();
        verify(enforcementOrderService, never()).saveAndClearDraftData(anyLong(), any());
    }

    @Test
    void submit_refusesAnApplicationWithNoEnforcementTypeSelected() {
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder().build())
            .build();

        SubmitResponse<State> response = event.submit(payload(enforcementOrder));

        assertThat(response.getErrors()).containsExactly(EnforcementSubmitEvent.NO_ENFORCEMENT_TYPE_ERROR);
        verify(enforcementOrderService, never()).saveAndClearDraftData(anyLong(), any());
    }

    @Test
    void submit_refusesAnApplicationWithNoEnforcementOrder() {
        SubmitResponse<State> response = event.submit(payload(null));

        assertThat(response.getErrors()).containsExactly(EnforcementSubmitEvent.NO_ENFORCEMENT_TYPE_ERROR);
        verify(enforcementOrderService, never()).saveAndClearDraftData(anyLong(), any());
    }

    @ParameterizedTest
    @EnumSource(SelectEnforcementType.class)
    void submit_refusesASecondLiveEnforcement(SelectEnforcementType enforcementType) {
        when(enforcementOrderService.hasLiveEnforcement(CASE_REFERENCE)).thenReturn(true);

        SubmitResponse<State> response = event.submit(payload(enforcementOrder(enforcementType)));

        assertThat(response.getErrors()).containsExactly(LiveEnforcementExistsException.MESSAGE);
        assertThat(response.getState()).isNull();
        assertThat(response.getConfirmationBody()).isNull();
        verify(enforcementOrderService, never()).saveAndClearDraftData(anyLong(), any());
    }

    private static EnforcementOrder enforcementOrder(SelectEnforcementType enforcementType) {
        return EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
                                       .value(DynamicStringListElement.builder()
                                                  .code(enforcementType.name())
                                                  .build())
                                       .build())
            .build();
    }

    private static EventPayload<PCSCase, State> payload(EnforcementOrder enforcementOrder) {
        return new EventPayload<>(CASE_REFERENCE,
                                  PCSCase.builder().enforcementOrder(enforcementOrder).build(),
                                  null);
    }

}
