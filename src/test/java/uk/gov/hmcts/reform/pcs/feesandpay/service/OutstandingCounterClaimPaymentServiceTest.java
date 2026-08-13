package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.OutstandingCounterClaimPayment;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutstandingCounterClaimPaymentServiceTest {

    private static final long CASE_REFERENCE = 12_345_678L;
    private static final UUID PARTY_ID = UUID.randomUUID();
    private static final UUID IDAM_USER_ID = UUID.randomUUID();
    private static final UUID COUNTER_CLAIM_ID = UUID.randomUUID();
    private static final String SERVICE_REQUEST_REFERENCE = "2026-1234567890123";

    @Mock
    private CounterClaimRepository counterClaimRepository;

    @Mock
    private FeePaymentRepository feePaymentRepository;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private DefendantAccessValidator defendantAccessValidator;

    @InjectMocks
    private OutstandingCounterClaimPaymentService underTest;

    @Test
    void shouldReturnOutstandingPaymentWhenPendingCounterClaimHasFeePayment() {
        CounterClaimEntity counterClaim = pendingCounterClaim(null);
        counterClaim.setClaimType(CounterClaimType.PAYMENT_OR_COMPENSATION);
        counterClaim.setIsClaimAmountKnown(VerticalYesNo.YES);
        counterClaim.setClaimAmount(new BigDecimal("649.00"));
        FeePaymentEntity feePayment = FeePaymentEntity.builder()
            .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
            .amount(new BigDecimal("404.00"))
            .build();

        when(counterClaimRepository.findFirstByPcsCaseCaseReferenceAndPartyIdAndStatusOrderByClaimSubmittedDateDesc(
            CASE_REFERENCE, PARTY_ID, CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED
        )).thenReturn(Optional.of(counterClaim));
        when(feePaymentRepository.findByRelatedEntityId(COUNTER_CLAIM_ID)).thenReturn(Optional.of(feePayment));

        Optional<OutstandingCounterClaimPayment> result =
            underTest.findOutstandingPaymentForParty(CASE_REFERENCE, PARTY_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getServiceRequestReference()).isEqualTo(SERVICE_REQUEST_REFERENCE);
        assertThat(result.get().getFeeAmount()).isEqualByComparingTo("404.00");
        assertThat(result.get().getCounterClaimAmountInPence()).isEqualTo("64900");
        assertThat(result.get().getCounterClaimType()).isEqualTo("PAYMENT_OR_COMPENSATION");
    }

    @Test
    void shouldReturnEmptyWhenCounterClaimHasHwfReference() {
        CounterClaimEntity counterClaim = pendingCounterClaim("HWF-123456");

        when(counterClaimRepository.findFirstByPcsCaseCaseReferenceAndPartyIdAndStatusOrderByClaimSubmittedDateDesc(
            CASE_REFERENCE, PARTY_ID, CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED
        )).thenReturn(Optional.of(counterClaim));

        Optional<OutstandingCounterClaimPayment> result =
            underTest.findOutstandingPaymentForParty(CASE_REFERENCE, PARTY_ID);

        assertThat(result).isEmpty();
        verifyNoInteractions(feePaymentRepository);
    }

    @Test
    void shouldReturnEmptyWhenNoPendingCounterClaim() {
        when(counterClaimRepository.findFirstByPcsCaseCaseReferenceAndPartyIdAndStatusOrderByClaimSubmittedDateDesc(
            CASE_REFERENCE, PARTY_ID, CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED
        )).thenReturn(Optional.empty());

        assertThat(underTest.findOutstandingPaymentForParty(CASE_REFERENCE, PARTY_ID)).isEmpty();
        verifyNoInteractions(feePaymentRepository);
    }

    @Test
    void shouldReturnEmptyWhenPartyIdIsNull() {
        assertThat(underTest.findOutstandingPaymentForParty(CASE_REFERENCE, null)).isEmpty();
        verifyNoInteractions(counterClaimRepository, feePaymentRepository);
    }

    @Test
    void shouldReturnEmptyWhenFeePaymentMissingServiceRequest() {
        CounterClaimEntity counterClaim = pendingCounterClaim(null);
        FeePaymentEntity feePayment = FeePaymentEntity.builder()
            .serviceRequestReference(" ")
            .amount(new BigDecimal("404.00"))
            .build();

        when(counterClaimRepository.findFirstByPcsCaseCaseReferenceAndPartyIdAndStatusOrderByClaimSubmittedDateDesc(
            CASE_REFERENCE, PARTY_ID, CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED
        )).thenReturn(Optional.of(counterClaim));
        when(feePaymentRepository.findByRelatedEntityId(COUNTER_CLAIM_ID)).thenReturn(Optional.of(feePayment));

        assertThat(underTest.findOutstandingPaymentForParty(CASE_REFERENCE, PARTY_ID)).isEmpty();
    }

    @Test
    void shouldGetOutstandingForAuthenticatedDefendant() {
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PartyEntity defendant = PartyEntity.builder().id(PARTY_ID).idamId(IDAM_USER_ID).build();
        CounterClaimEntity counterClaim = pendingCounterClaim(null);
        FeePaymentEntity feePayment = FeePaymentEntity.builder()
            .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
            .amount(new BigDecimal("80.00"))
            .build();

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(defendantAccessValidator.validateAndGetDefendant(caseEntity, IDAM_USER_ID)).thenReturn(defendant);
        when(counterClaimRepository.findFirstByPcsCaseCaseReferenceAndPartyIdAndStatusOrderByClaimSubmittedDateDesc(
            CASE_REFERENCE, PARTY_ID, CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED
        )).thenReturn(Optional.of(counterClaim));
        when(feePaymentRepository.findByRelatedEntityId(COUNTER_CLAIM_ID)).thenReturn(Optional.of(feePayment));

        OutstandingCounterClaimPayment result =
            underTest.getOutstandingForDefendant(CASE_REFERENCE, IDAM_USER_ID);

        assertThat(result.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST_REFERENCE);
        assertThat(result.getFeeAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void shouldThrowWhenNoOutstandingPaymentForDefendant() {
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PartyEntity defendant = PartyEntity.builder().id(PARTY_ID).idamId(IDAM_USER_ID).build();

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(defendantAccessValidator.validateAndGetDefendant(caseEntity, IDAM_USER_ID)).thenReturn(defendant);
        when(counterClaimRepository.findFirstByPcsCaseCaseReferenceAndPartyIdAndStatusOrderByClaimSubmittedDateDesc(
            CASE_REFERENCE, PARTY_ID, CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getOutstandingForDefendant(CASE_REFERENCE, IDAM_USER_ID))
            .isInstanceOf(FeePaymentNotFoundException.class)
            .hasMessageContaining(String.valueOf(CASE_REFERENCE));
    }

    private static CounterClaimEntity pendingCounterClaim(String hwfReferenceNumber) {
        return CounterClaimEntity.builder()
            .id(COUNTER_CLAIM_ID)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .hwfReferenceNumber(hwfReferenceNumber)
            .build();
    }
}
