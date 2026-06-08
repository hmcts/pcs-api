package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimPaymentCallbackHandlerTest {

    @Mock
    private CounterClaimRepository counterClaimRepository;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private CounterClaimPaymentCallbackHandler underTest;

    private static final Clock FIXED_UTC_CLOCK = Clock.fixed(
        Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        underTest = new CounterClaimPaymentCallbackHandler(counterClaimRepository, objectMapper, FIXED_UTC_CLOCK);
    }

    @Test
    void shouldSetCounterClaimIssuedWhenPaymentIsPaid() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED)
            .party(PartyEntity.builder().id(partyId).build())
            .build();

        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeDetails(FeeDetails.builder().feeAmount(BigDecimal.TEN).build())
            .caseReference(1234567890123456L)
            .ccdCaseNumber("1234567890123456")
            .responsiblePartyId(partyId)
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE)
            .relatedEntityId(counterClaimId)
            .build();

        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.PAID)
            .taskData("task-data")
            .build();

        PaymentStatusCallback callback = PaymentStatusCallback.builder()
            .serviceRequestStatus(PaymentStatus.PAID.getValue())
            .payment(Payment.builder().paymentReference("RC-123").build())
            .build();

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);

        underTest.handle(callback, feePaymentEntity);

        assertThat(feePaymentEntity.getParty()).isEqualTo(counterClaimEntity.getParty());
        ArgumentCaptor<CounterClaimEntity> captor = ArgumentCaptor.forClass(CounterClaimEntity.class);
        verify(counterClaimRepository).save(captor.capture());
        CounterClaimEntity savedCounterClaim = captor.getValue();
        assertThat(savedCounterClaim.getStatus()).isEqualTo(CounterClaimStatus.COUNTER_CLAIM_ISSUED);
        assertThat(savedCounterClaim.getClaimIssuedDate()).isEqualTo(LocalDateTime.of(2026, 6, 1, 10, 0));
    }

    @Test
    void shouldNotIssueCounterClaimWhenAlreadyIssued() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        LocalDateTime existingIssuedDate = LocalDateTime.of(2026, 5, 1, 9, 0);

        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimStatus.COUNTER_CLAIM_ISSUED)
            .claimIssuedDate(existingIssuedDate)
            .party(PartyEntity.builder().id(partyId).build())
            .build();

        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeDetails(FeeDetails.builder().feeAmount(BigDecimal.TEN).build())
            .caseReference(1234567890123456L)
            .ccdCaseNumber("1234567890123456")
            .responsiblePartyId(partyId)
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE)
            .relatedEntityId(counterClaimId)
            .build();

        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.PAID)
            .taskData("task-data")
            .build();

        PaymentStatusCallback callback = PaymentStatusCallback.builder()
            .serviceRequestStatus(PaymentStatus.PAID.getValue())
            .payment(Payment.builder().paymentReference("RC-126").build())
            .build();

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);

        underTest.handle(callback, feePaymentEntity);

        assertThat(feePaymentEntity.getParty()).isEqualTo(counterClaimEntity.getParty());
        assertThat(counterClaimEntity.getStatus()).isEqualTo(CounterClaimStatus.COUNTER_CLAIM_ISSUED);
        assertThat(counterClaimEntity.getClaimIssuedDate()).isEqualTo(existingIssuedDate);
        verify(counterClaimRepository, never()).save(counterClaimEntity);
    }

    @Test
    void shouldNotUpdateCounterClaimWhenPaymentIsNotPaid() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED)
            .party(PartyEntity.builder().id(partyId).build())
            .build();

        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeDetails(FeeDetails.builder().feeAmount(BigDecimal.TEN).build())
            .caseReference(1234567890123456L)
            .ccdCaseNumber("1234567890123456")
            .responsiblePartyId(partyId)
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE)
            .relatedEntityId(counterClaimId)
            .build();

        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.NOT_PAID)
            .taskData("task-data")
            .build();

        PaymentStatusCallback callback = PaymentStatusCallback.builder()
            .serviceRequestStatus(PaymentStatus.NOT_PAID.getValue())
            .payment(Payment.builder().paymentReference("RC-124").build())
            .build();

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);

        underTest.handle(callback, feePaymentEntity);

        assertThat(feePaymentEntity.getParty()).isEqualTo(counterClaimEntity.getParty());
        verify(counterClaimRepository, never()).save(counterClaimEntity);
    }

    @Test
    void shouldThrowWhenTaskDataDoesNotContainCounterClaimId() throws Exception {
        UUID partyId = UUID.randomUUID();
        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeDetails(FeeDetails.builder().feeAmount(BigDecimal.TEN).build())
            .caseReference(1234567890123456L)
            .ccdCaseNumber("1234567890123456")
            .responsiblePartyId(partyId)
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE)
            .relatedEntityId(null)
            .build();
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.PAID)
            .taskData("task-data")
            .build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder()
            .serviceRequestStatus(PaymentStatus.PAID.getValue())
            .payment(Payment.builder().paymentReference("RC-125").build())
            .build();

        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);

        assertThatThrownBy(() -> underTest.handle(callback, feePaymentEntity))
            .isInstanceOf(PaymentCallbackException.class)
            .hasMessageContaining("missing relatedEntityId");
    }
}
