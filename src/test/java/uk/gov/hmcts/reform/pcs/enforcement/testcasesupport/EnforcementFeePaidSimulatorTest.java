package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementPaymentCallbackHandler;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementFeePaidSimulatorTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID CLAIM_ID = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
    private static final UUID ENFORCEMENT_ID = UUID.fromString("0f0e0d0c-0b0a-0908-0706-050403020100");
    private static final String SERVICE_REQUEST = "SR-2026-1692266328473";

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private FeePaymentRepository feePaymentRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private EnforcementPaymentCallbackHandler enforcementPaymentCallbackHandler;

    @InjectMocks
    private EnforcementFeePaidSimulator underTest;

    private final EnforcementOrderEntity enforcement = new EnforcementOrderEntity();

    @BeforeEach
    void givenACase() {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        ClaimEntity claim = new ClaimEntity();
        claim.setId(CLAIM_ID);
        pcsCase.addClaim(claim);
        enforcement.setId(ENFORCEMENT_ID);
        enforcement.setClaim(claim);
        enforcement.setStatus(EnforcementStatus.AWAITING_PAYMENT);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
    }

    @Test
    void shouldSendAPaidCallbackForTheEnforcementsServiceRequest() {
        givenAnApplicationAwaitingPayment();
        when(feePaymentRepository.findByRelatedEntityId(ENFORCEMENT_ID)).thenReturn(Optional.of(
            FeePaymentEntity.builder()
                .serviceRequestReference(SERVICE_REQUEST)
                .paymentCallbackHandlerType(PaymentCallbackHandlerType.ENFORCEMENT)
                .relatedEntityId(ENFORCEMENT_ID)
                .build()));

        assertThat(underTest.recordFeePaid(CASE_REFERENCE)).isTrue();

        ArgumentCaptor<PaymentStatusCallback> callback = ArgumentCaptor.forClass(PaymentStatusCallback.class);
        verify(paymentService).processPaymentResponse(callback.capture());
        assertThat(callback.getValue().getServiceRequestReference()).isEqualTo(SERVICE_REQUEST);
        assertThat(callback.getValue().getCcdCaseNumber()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(callback.getValue().getServiceRequestStatus()).isEqualTo("Paid");
        verifyNoInteractions(enforcementPaymentCallbackHandler);
    }

    @Test
    void shouldPayThroughTheHandlerWhenNoServiceRequestWasRaised() {
        givenAnApplicationAwaitingPayment();
        when(feePaymentRepository.findByRelatedEntityId(ENFORCEMENT_ID)).thenReturn(Optional.empty());

        assertThat(underTest.recordFeePaid(CASE_REFERENCE)).isTrue();

        ArgumentCaptor<FeePaymentEntity> feePayment = ArgumentCaptor.forClass(FeePaymentEntity.class);
        verify(enforcementPaymentCallbackHandler).handle(any(PaymentStatusCallback.class), feePayment.capture());
        assertThat(feePayment.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(feePayment.getValue().getRelatedEntityId()).isEqualTo(ENFORCEMENT_ID);
        assertThat(feePayment.getValue().getPaymentCallbackHandlerType())
            .isEqualTo(PaymentCallbackHandlerType.ENFORCEMENT);
        verify(paymentService, never()).processPaymentResponse(any());
        verify(feePaymentRepository, never()).save(any());
    }

    @Test
    void shouldRecordNoFeeWhenNoApplicationIsAwaitingPayment() {
        when(enforcementOrderRepository.findByClaimIdAndStatus(CLAIM_ID, EnforcementStatus.AWAITING_PAYMENT))
            .thenReturn(List.of());

        assertThat(underTest.recordFeePaid(CASE_REFERENCE)).isFalse();

        verifyNoInteractions(feePaymentRepository, paymentService, enforcementPaymentCallbackHandler);
    }

    private void givenAnApplicationAwaitingPayment() {
        when(enforcementOrderRepository.findByClaimIdAndStatus(CLAIM_ID, EnforcementStatus.AWAITING_PAYMENT))
            .thenReturn(List.of(enforcement));
    }
}
