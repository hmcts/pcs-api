package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.SystemEventAction;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.genapp.GenAppWaTaskService;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.exception.GenAppNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult.Outcome.EXECUTED;
import static uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult.Outcome.REPLAYED;

@ExtendWith(MockitoExtension.class)
class GenAppPaymentCallbackHandlerTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String SERVICE_REQUEST_REFERENCE = "2026-1750000000000";
    private static final String PAYMENT_REFERENCE = "RC-1111-2222-3333-4444";
    private static final UUID EXPECTED_IDEMPOTENCY_KEY = UUID.nameUUIDFromBytes(
        ("genAppIssued:" + SERVICE_REQUEST_REFERENCE).getBytes(StandardCharsets.UTF_8));

    @Mock
    private SystemEventExecutor systemEventExecutor;
    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private FeePaymentRepository feePaymentRepository;
    @Mock
    private GenAppDocumentGenerator genAppDocumentGenerator;
    @Mock
    private NotificationService notificationService;
    @Mock
    private GenAppWaTaskService genAppWaTaskService;

    @InjectMocks
    private GenAppPaymentCallbackHandler underTest;

    @Test
    void paidCallback_RecordsGenAppIssuedSystemEventAndRunsSideEffects() {
        // given
        UUID genAppId = UUID.randomUUID();
        GenAppEntity genAppEntity = pendingGenApp();
        FeePaymentEntity feePaymentEntity = feePayment(genAppId);
        when(genAppRepository.findById(genAppId)).thenReturn(Optional.of(genAppEntity));
        when(systemEventExecutor.execute(eq(CASE_REFERENCE), any(UUID.class), any()))
            .thenReturn(new SystemEventExecutionResult(1L, EXECUTED));

        // when
        underTest.handle(paidCallback(), feePaymentEntity);

        // then - the executor is given the change keyed by the service request reference
        ArgumentCaptor<SystemEventAction> actionCaptor = ArgumentCaptor.forClass(SystemEventAction.class);
        verify(systemEventExecutor).execute(eq(CASE_REFERENCE), eq(EXPECTED_IDEMPOTENCY_KEY),
                                            actionCaptor.capture());

        // running the action applies the fee update, transitions the gen app and describes the event
        SystemEventResult result = actionCaptor.getValue().execute(null);
        assertThat(feePaymentEntity.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(feePaymentEntity.getExternalReference()).isEqualTo(PAYMENT_REFERENCE);
        verify(feePaymentRepository).save(feePaymentEntity);
        assertThat(genAppEntity.getState()).isEqualTo(GenAppState.GEN_APP_ISSUED);
        verify(genAppRepository).save(genAppEntity);
        assertThat(result.eventId()).isEqualTo("genAppIssued");
        assertThat(result.eventName()).isEqualTo("General application issued");
        assertThat(result.state()).isEmpty();

        // side effects run once, after commit
        verify(genAppDocumentGenerator).createSubmissionDocument(CASE_REFERENCE, genAppEntity);
        verify(notificationService).sendGenAppReceivedEmail(genAppEntity);
        verify(genAppWaTaskService).createReviewGenAppTask(CASE_REFERENCE, genAppEntity);
        verify(genAppWaTaskService).createTranslationTaskForGenApp(genAppEntity);
    }

    @Test
    void replayedCallback_SkipsSideEffects() {
        // given
        UUID genAppId = UUID.randomUUID();
        GenAppEntity genAppEntity = pendingGenApp();
        FeePaymentEntity feePaymentEntity = feePayment(genAppId);
        when(genAppRepository.findById(genAppId)).thenReturn(Optional.of(genAppEntity));
        when(systemEventExecutor.execute(eq(CASE_REFERENCE), any(UUID.class), any()))
            .thenReturn(new SystemEventExecutionResult(1L, REPLAYED));

        // when
        underTest.handle(paidCallback(), feePaymentEntity);

        // then - a replay re-runs nothing external
        verifyNoInteractions(genAppDocumentGenerator, notificationService, genAppWaTaskService);
    }

    @Test
    void notPaidCallback_RecordsNoEventAndNoSideEffects() {
        // given
        UUID genAppId = UUID.randomUUID();
        FeePaymentEntity feePaymentEntity = feePayment(genAppId);

        // when
        underTest.handle(callbackWithStatus("Not paid"), feePaymentEntity);

        // then
        verifyNoInteractions(systemEventExecutor, genAppRepository, feePaymentRepository,
                             genAppDocumentGenerator, notificationService, genAppWaTaskService);
    }

    @Test
    void alreadyIssued_RecordsNoEventAndNoSideEffects() {
        // given
        UUID genAppId = UUID.randomUUID();
        GenAppEntity genAppEntity = GenAppEntity.builder().state(GenAppState.GEN_APP_ISSUED).build();
        FeePaymentEntity feePaymentEntity = feePayment(genAppId);
        when(genAppRepository.findById(genAppId)).thenReturn(Optional.of(genAppEntity));

        // when
        underTest.handle(paidCallback(), feePaymentEntity);

        // then
        verifyNoInteractions(systemEventExecutor, feePaymentRepository,
                             genAppDocumentGenerator, notificationService, genAppWaTaskService);
    }

    @Test
    void unknownGenApp_Throws() {
        // given
        UUID unknownGenAppId = UUID.randomUUID();
        FeePaymentEntity feePaymentEntity = feePayment(unknownGenAppId);
        when(genAppRepository.findById(unknownGenAppId)).thenReturn(Optional.empty());

        // when
        Throwable throwable = catchThrowable(() -> underTest.handle(paidCallback(), feePaymentEntity));

        // then
        assertThat(throwable).isInstanceOf(GenAppNotFoundException.class);
        verifyNoInteractions(systemEventExecutor);
    }

    @Test
    void handlesItsOwnTransactionOnlyWhenPaid() {
        assertThat(underTest.handlesOwnTransaction(paidCallback())).isTrue();
        assertThat(underTest.handlesOwnTransaction(callbackWithStatus("Partially paid"))).isFalse();
        assertThat(underTest.handlesOwnTransaction(callbackWithStatus("Not paid"))).isFalse();
    }

    private GenAppEntity pendingGenApp() {
        return GenAppEntity.builder()
            .state(GenAppState.PENDING_GEN_APP_ISSUED)
            .pcsCase(PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build())
            .build();
    }

    private FeePaymentEntity feePayment(UUID genAppId) {
        return FeePaymentEntity.builder()
            .relatedEntityId(genAppId)
            .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
            .build();
    }

    private PaymentStatusCallback paidCallback() {
        return callbackWithStatus("Paid");
    }

    private PaymentStatusCallback callbackWithStatus(String status) {
        return PaymentStatusCallback.builder()
            .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
            .serviceRequestStatus(status)
            .payment(Payment.builder().paymentReference(PAYMENT_REFERENCE).build())
            .build();
    }
}
