package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardPaymentStatusPollProcessorTest {

    private static final String SERVICE_REQUEST_REFERENCE = "2026-1750003537986";
    private static final String PAYMENT_REFERENCE = "RC-1781-1028-7002-7072";

    @Mock
    private FeePaymentRepository feePaymentRepository;
    @Mock
    private ObjectProvider<PaymentService> paymentServiceProvider;
    @Mock
    private PaymentService paymentService;

    private CardPaymentStatusPollProcessor underTest;

    @BeforeEach
    void setUp() {
        underTest = new CardPaymentStatusPollProcessor(feePaymentRepository, paymentServiceProvider);
        lenient().when(paymentServiceProvider.getObject()).thenReturn(paymentService);
    }

    @Test
    @DisplayName("Should process paid status when fee payment is still pending")
    void shouldProcessPaidStatusWhenFeePaymentPending() {
        PaymentDto paymentDto = PaymentDto.builder()
            .status("Success")
            .paymentGroupReference(SERVICE_REQUEST_REFERENCE)
            .reference(PAYMENT_REFERENCE)
            .ccdCaseNumber("1781197624030141")
            .amount(new BigDecimal("35.00"))
            .build();

        when(feePaymentRepository.findByServiceRequestReference(SERVICE_REQUEST_REFERENCE))
            .thenReturn(Optional.of(FeePaymentEntity.builder()
                .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
                .paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE)
                .build()));

        underTest.processIfPaid(paymentDto);

        ArgumentCaptor<PaymentStatusCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentStatusCallback.class);
        verify(paymentService).processPaymentResponse(callbackCaptor.capture());

        PaymentStatusCallback callback = callbackCaptor.getValue();
        assertThat(callback.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST_REFERENCE);
        assertThat(callback.getServiceRequestStatus()).isEqualTo(PaymentStatus.PAID.getValue());
        assertThat(callback.getPaymentReference()).isEqualTo(PAYMENT_REFERENCE);
    }

    @Test
    @DisplayName("Should skip processing when PayHub status is not successful")
    void shouldSkipWhenStatusNotSuccessful() {
        underTest.processIfPaid(PaymentDto.builder()
            .status("Failed")
            .paymentGroupReference(SERVICE_REQUEST_REFERENCE)
            .build());

        verify(feePaymentRepository, never()).findByServiceRequestReference(any());
        verify(paymentService, never()).processPaymentResponse(any());
    }

    @Test
    @DisplayName("Should skip processing when fee payment is already paid")
    void shouldSkipWhenFeePaymentAlreadyPaid() {
        when(feePaymentRepository.findByServiceRequestReference(SERVICE_REQUEST_REFERENCE))
            .thenReturn(Optional.of(FeePaymentEntity.builder()
                .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
                .paymentStatus(PaymentStatus.PAID)
                .build()));

        underTest.processIfPaid(PaymentDto.builder()
            .status("Success")
            .paymentGroupReference(SERVICE_REQUEST_REFERENCE)
            .build());

        verify(paymentService, never()).processPaymentResponse(any());
    }

    @Test
    @DisplayName("Should skip processing when payment group reference is missing")
    void shouldSkipWhenPaymentGroupReferenceMissing() {
        underTest.processIfPaid(PaymentDto.builder().status("Success").build());

        verify(feePaymentRepository, never()).findByServiceRequestReference(any());
        verify(paymentService, never()).processPaymentResponse(any());
    }

    @Test
    @DisplayName("Should skip processing when payment group reference is blank")
    void shouldSkipWhenPaymentGroupReferenceBlank() {
        underTest.processIfPaid(PaymentDto.builder()
            .status("Success")
            .paymentGroupReference("   ")
            .build());

        verify(feePaymentRepository, never()).findByServiceRequestReference(any());
        verify(paymentService, never()).processPaymentResponse(any());
    }

    @Test
    @DisplayName("Should skip processing when no fee payment exists for service request")
    void shouldSkipWhenNoFeePaymentFound() {
        when(feePaymentRepository.findByServiceRequestReference(SERVICE_REQUEST_REFERENCE))
            .thenReturn(Optional.empty());

        underTest.processIfPaid(PaymentDto.builder()
            .status("Success")
            .paymentGroupReference(SERVICE_REQUEST_REFERENCE)
            .build());

        verify(paymentService, never()).processPaymentResponse(any());
    }

    @Test
    @DisplayName("Should use paymentReference when reference is missing")
    void shouldUsePaymentReferenceWhenReferenceMissing() {
        when(feePaymentRepository.findByServiceRequestReference(SERVICE_REQUEST_REFERENCE))
            .thenReturn(Optional.of(FeePaymentEntity.builder()
                .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
                .paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE)
                .build()));

        underTest.processIfPaid(PaymentDto.builder()
            .status("Success")
            .paymentGroupReference(SERVICE_REQUEST_REFERENCE)
            .paymentReference(PAYMENT_REFERENCE)
            .build());

        ArgumentCaptor<PaymentStatusCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentStatusCallback.class);
        verify(paymentService).processPaymentResponse(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().getPaymentReference()).isEqualTo(PAYMENT_REFERENCE);
    }

    @Test
    @DisplayName("Should use paymentReference when reference is blank")
    void shouldUsePaymentReferenceWhenReferenceBlank() {
        when(feePaymentRepository.findByServiceRequestReference(SERVICE_REQUEST_REFERENCE))
            .thenReturn(Optional.of(FeePaymentEntity.builder()
                .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
                .paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE)
                .build()));

        underTest.processIfPaid(PaymentDto.builder()
            .status("Success")
            .paymentGroupReference(SERVICE_REQUEST_REFERENCE)
            .reference("   ")
            .paymentReference(PAYMENT_REFERENCE)
            .build());

        ArgumentCaptor<PaymentStatusCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentStatusCallback.class);
        verify(paymentService).processPaymentResponse(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().getPaymentReference()).isEqualTo(PAYMENT_REFERENCE);
    }

    @Test
    @DisplayName("Should use externalReference when reference fields are missing")
    void shouldUseExternalReferenceWhenReferenceFieldsMissing() {
        when(feePaymentRepository.findByServiceRequestReference(SERVICE_REQUEST_REFERENCE))
            .thenReturn(Optional.of(FeePaymentEntity.builder()
                .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
                .paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE)
                .build()));

        underTest.processIfPaid(PaymentDto.builder()
            .status("Success")
            .paymentGroupReference(SERVICE_REQUEST_REFERENCE)
            .externalReference(PAYMENT_REFERENCE)
            .build());

        ArgumentCaptor<PaymentStatusCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentStatusCallback.class);
        verify(paymentService).processPaymentResponse(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().getPaymentReference()).isEqualTo(PAYMENT_REFERENCE);
    }

    @Test
    @DisplayName("Should use externalReference when reference fields are blank")
    void shouldUseExternalReferenceWhenReferenceFieldsBlank() {
        when(feePaymentRepository.findByServiceRequestReference(SERVICE_REQUEST_REFERENCE))
            .thenReturn(Optional.of(FeePaymentEntity.builder()
                .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
                .paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE)
                .build()));

        underTest.processIfPaid(PaymentDto.builder()
            .status("Success")
            .paymentGroupReference(SERVICE_REQUEST_REFERENCE)
            .reference("   ")
            .paymentReference("   ")
            .externalReference(PAYMENT_REFERENCE)
            .build());

        ArgumentCaptor<PaymentStatusCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentStatusCallback.class);
        verify(paymentService).processPaymentResponse(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().getPaymentReference()).isEqualTo(PAYMENT_REFERENCE);
    }
}
