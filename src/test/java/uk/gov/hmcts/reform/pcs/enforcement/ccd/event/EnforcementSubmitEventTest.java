package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.exception.LiveEnforcementExistsException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementSideEffects;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_ENFORCEMENT_ISSUED;

@ExtendWith(MockitoExtension.class)
class EnforcementSubmitEventTest {

    private static final long CASE_REFERENCE = 123456789L;
    private static final FeeDetails FEE = FeeDetails.builder()
        .code("FEE0380")
        .feeAmount(new BigDecimal("148.00"))
        .version(5)
        .build();

    /**
     * An application that owes nothing: a change that was made with no fee to report.
     */
    private static final FeeDetails NO_FEE = null;

    @Mock
    private EnforcementOrderService enforcementOrderService;
    @Spy
    private MoneyFormatter moneyFormatter = new MoneyFormatter();

    @InjectMocks
    private EnforcementSubmitEvent event;

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT", "WRIT"})
    void submit_savesEnforcementOrderAndAsksForTheFee(SelectEnforcementType enforcementType) {
        EnforcementOrder enforcementOrder = enforcementOrder(enforcementType);
        // The amount shown is the one the service request is raised for, not the one on the case data.
        enforcementOrder.setWarrantFeeAmount("£1");
        enforcementOrder.setWritFeeAmount("£1");
        when(enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder))
            .thenReturn(EnforcementChange.made(FEE));

        SubmitResponse<State> response = event.submit(payload(enforcementOrder));

        verify(enforcementOrderService).saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getState()).isEqualTo(PENDING_ENFORCEMENT_ISSUED);
        assertThat(response.getConfirmationHeader())
            .isEqualTo("# Pay £148 for HM Courts and Tribunals Service to review your application");
        assertThat(response.getConfirmationBody())
            .contains("This costs £148")
            .contains("/cases/case-details/%d#Service%%20Request".formatted(CASE_REFERENCE));
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT_OF_RESTITUTION", "WRIT_OF_RESTITUTION"})
    void submit_savesARestitutionApplicationWithoutAPaymentPanel(SelectEnforcementType enforcementType) {
        EnforcementOrder enforcementOrder = enforcementOrder(enforcementType);
        when(enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder))
            .thenReturn(EnforcementChange.made(NO_FEE));

        SubmitResponse<State> response = event.submit(payload(enforcementOrder));

        verify(enforcementOrderService).saveAndClearDraftData(CASE_REFERENCE, enforcementOrder);
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getState()).isEqualTo(PENDING_ENFORCEMENT_ISSUED);
        assertThat(response.getConfirmationHeader()).isNull();
        assertThat(response.getConfirmationBody()).isNull();
    }

    // 01 "Repeat warrant within 12 months of a suspended warrant is free" (HDPI-8424).
    @Test
    void submit_doesNotAskForTheFeeForAFreeRepeatWarrant() {
        EnforcementOrder enforcementOrder = enforcementOrder(SelectEnforcementType.WARRANT);
        enforcementOrder.setWarrantFeeAmount("£148");
        when(enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder))
            .thenReturn(EnforcementChange.made(NO_FEE));

        SubmitResponse<State> response = event.submit(payload(enforcementOrder));

        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getState()).isEqualTo(PENDING_ENFORCEMENT_ISSUED);
        assertThat(response.getConfirmationBody()).isNull();
    }

    @Test
    void submit_refusesAnApplicationWhoseFeeCannotBeLookedUp() {
        EnforcementOrder enforcementOrder = enforcementOrder(SelectEnforcementType.WARRANT);
        when(enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder))
            .thenThrow(new FeeNotFoundException("Unable to retrieve fee: ENFORCEMENT_WARRANT_FEE"));

        SubmitResponse<State> response = event.submit(payload(enforcementOrder));

        assertThat(response.getErrors()).containsExactly(EnforcementSubmitEvent.FEE_UNAVAILABLE_ERROR);
        assertThat(response.getState()).isNull();
        assertThat(response.getConfirmationBody()).isNull();
    }

    /**
     * The claimant is told to submit again rather than given an unexplained 500 on an application the service
     * could not queue the documents or the payment request for.
     */
    @Test
    void submit_refusesAnApplicationWhoseSideEffectsCannotBeScheduled() {
        EnforcementOrder enforcementOrder = enforcementOrder(SelectEnforcementType.WARRANT);
        when(enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder))
            .thenReturn(EnforcementChange.refused(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR));

        SubmitResponse<State> response = event.submit(payload(enforcementOrder));

        assertThat(response.getErrors()).containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
        assertThat(response.getState()).isNull();
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

    @Test
    void submit_refusesAnApplicationTheDatabaseRejectsAsASecondLiveEnforcement() {
        EnforcementOrder enforcementOrder = enforcementOrder(SelectEnforcementType.WARRANT);
        when(enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder))
            .thenThrow(new DataIntegrityViolationException(
                "could not execute statement", new ConstraintViolationException(
                    "unique constraint violated", new SQLException(),
                    LiveEnforcementExistsException.CONSTRAINT_NAME)));

        SubmitResponse<State> response = event.submit(payload(enforcementOrder));

        assertThat(response.getErrors()).containsExactly(LiveEnforcementExistsException.MESSAGE);
        assertThat(response.getState()).isNull();
    }

    @Test
    void submit_letsAnyOtherIntegrityViolationThrough() {
        EnforcementOrder enforcementOrder = enforcementOrder(SelectEnforcementType.WARRANT);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("enf_case.claim_id is null");
        when(enforcementOrderService.saveAndClearDraftData(CASE_REFERENCE, enforcementOrder)).thenThrow(failure);

        assertThatThrownBy(() -> event.submit(payload(enforcementOrder))).isSameAs(failure);
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
