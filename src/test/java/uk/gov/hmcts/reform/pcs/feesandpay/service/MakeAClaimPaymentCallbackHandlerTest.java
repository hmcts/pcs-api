package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.SystemEventAction;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseIssueService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MakeAClaimPaymentCallbackHandlerTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String SERVICE_REQUEST_REFERENCE = "2026-1750000000000";
    private static final String PAYMENT_REFERENCE = "RC-1111-2222-3333-4444";
    private static final UUID RESPONSIBLE_PARTY_ID = UUID.randomUUID();
    private static final UUID EXPECTED_IDEMPOTENCY_KEY = UUID.nameUUIDFromBytes(
        ("claimIssuePayment:" + SERVICE_REQUEST_REFERENCE).getBytes(StandardCharsets.UTF_8));

    @Mock
    private SystemEventExecutor systemEventExecutor;
    @Mock
    private CaseIssueService caseIssueService;
    @Mock
    private PartyService partyService;
    @Mock
    private FeePaymentRepository feePaymentRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MakeAClaimPaymentCallbackHandler underTest;

    @Test
    void paidCallback_RecordsAClaimIssuePaymentSystemEvent() throws Exception {
        // given
        FeesAndPayTaskData taskData = buildTaskData();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).build();
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE)).thenReturn(partyEntity);
        FeePaymentEntity feePaymentEntity = feePayment();

        // when
        underTest.handle(paidCallback(), feePaymentEntity);

        // then - the executor is given the change keyed by the service request reference
        ArgumentCaptor<SystemEventAction> actionCaptor = ArgumentCaptor.forClass(SystemEventAction.class);
        verify(systemEventExecutor).execute(eq(CASE_REFERENCE), eq(EXPECTED_IDEMPOTENCY_KEY),
                                            actionCaptor.capture());

        // running the action applies the fee update, issues the case and describes the event
        SystemEventResult result = actionCaptor.getValue().execute(null);
        assertThat(feePaymentEntity.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(feePaymentEntity.getExternalReference()).isEqualTo(PAYMENT_REFERENCE);
        assertThat(feePaymentEntity.getParty()).isSameAs(partyEntity);
        verify(feePaymentRepository).save(feePaymentEntity);
        verify(caseIssueService).issueCaseIfNotIssued(CASE_REFERENCE);
        assertThat(result.eventId()).isEqualTo("claimIssuePayment");
        assertThat(result.eventName()).isEqualTo("Payment Confirmation");
        assertThat(result.state().orElseThrow()).isEqualTo(State.CASE_ISSUED);
    }

    @Test
    void notPaidCallback_SetsThePartyAndRecordsNoEvent() throws Exception {
        // given
        FeesAndPayTaskData taskData = buildTaskData();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).build();
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE)).thenReturn(partyEntity);
        FeePaymentEntity feePaymentEntity = feePayment();

        // when
        underTest.handle(callbackWithStatus("Not paid"), feePaymentEntity);

        // then
        assertThat(feePaymentEntity.getParty()).isSameAs(partyEntity);
        verifyNoInteractions(systemEventExecutor, caseIssueService, feePaymentRepository);
    }

    @Test
    void handlesItsOwnTransactionOnlyWhenPaid() {
        assertThat(underTest.handlesOwnTransaction(paidCallback())).isTrue();
        assertThat(underTest.handlesOwnTransaction(callbackWithStatus("Partially paid"))).isFalse();
        assertThat(underTest.handlesOwnTransaction(callbackWithStatus("Not paid"))).isFalse();
    }

    @Test
    void shouldThrowPaymentCallbackExceptionWhenTaskDataIsInvalidJson() throws Exception {
        // given
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class)))
            .thenThrow(new JsonProcessingException("Invalid JSON") {});
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().claim(new ClaimEntity()).taskData("aasdfsdf{{")
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();

        // when / then
        assertThatExceptionOfType(PaymentCallbackException.class)
            .isThrownBy(() -> underTest.handle(paidCallback(), feePaymentEntity))
            .withMessageContaining("Unable to process");
        verifyNoInteractions(systemEventExecutor, caseIssueService);
    }

    @Test
    void shouldPropagatePartyNotFoundExceptionWhenNoPartyMatchesResponsibleParty() throws Exception {
        // given
        FeesAndPayTaskData taskData = buildTaskData();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE))
            .thenThrow(new PartyNotFoundException("no party"));

        // when / then
        assertThatExceptionOfType(PartyNotFoundException.class)
            .isThrownBy(() -> underTest.handle(paidCallback(), feePayment()));
        verifyNoInteractions(systemEventExecutor, caseIssueService);
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

    private FeePaymentEntity feePayment() {
        ClaimEntity claimEntity = new ClaimEntity();
        claimEntity.setPcsCase(PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build());
        return FeePaymentEntity.builder()
            .claim(claimEntity)
            .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
            .taskData("{}")
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM)
            .build();
    }

    private FeesAndPayTaskData buildTaskData() {
        return FeesAndPayTaskData.builder()
            .caseReference(CASE_REFERENCE)
            .ccdCaseNumber(String.valueOf(CASE_REFERENCE))
            .responsiblePartyId(RESPONSIBLE_PARTY_ID)
            .responsiblePartyName("Claimant Org Ltd")
            .feeDetails(FeeDetails.builder().build())
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM)
            .build();
    }
}
