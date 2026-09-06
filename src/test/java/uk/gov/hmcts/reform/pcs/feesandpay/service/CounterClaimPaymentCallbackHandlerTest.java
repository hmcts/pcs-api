package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.SystemEventAction;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormScheduler;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult.Outcome.EXECUTED;
import static uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult.Outcome.REPLAYED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.COUNTER_CLAIM_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.task.CounterClaimIssuedNotificationTaskComponent.COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class CounterClaimPaymentCallbackHandlerTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String SERVICE_REQUEST_REFERENCE = "2026-1750000000000";
    private static final String PAYMENT_REFERENCE = "RC-1111-2222-3333-4444";
    private static final Clock FIXED_UTC_CLOCK = Clock.fixed(
        Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private SystemEventExecutor systemEventExecutor;
    @Mock
    private CounterClaimRepository counterClaimRepository;
    @Mock
    private FeePaymentRepository feePaymentRepository;
    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private CounterClaimFormScheduler counterClaimFormScheduler;
    @Mock
    private TranslationWAService translationWAService;
    @Mock
    private ObjectMapper objectMapper;
    @Captor
    private ArgumentCaptor<SchedulableInstance<CounterClaimTaskData>> taskInstanceCaptor;

    private CounterClaimPaymentCallbackHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new CounterClaimPaymentCallbackHandler(systemEventExecutor, counterClaimRepository,
                                                           feePaymentRepository, schedulerClient,
                                                           counterClaimFormScheduler, translationWAService,
                                                           objectMapper, FIXED_UTC_CLOCK);
    }

    @Test
    void paidCallback_RecordsCounterClaimIssuedSystemEventAndRunsSideEffects() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        CounterClaimEntity counterClaimEntity = createCounterClaimEntity(counterClaimId,
                                                                         PENDING_COUNTER_CLAIM_ISSUED, partyId);
        stubTaskData(partyId, counterClaimId);
        FeePaymentEntity feePaymentEntity = feePayment();
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(systemEventExecutor.execute(eq(CASE_REFERENCE), any(UUID.class), any()))
            .thenReturn(new SystemEventExecutionResult(1L, EXECUTED));

        underTest.handle(paidCallback(), feePaymentEntity);

        // the executor is given the change keyed by the service request reference
        UUID expectedKey = UUID.nameUUIDFromBytes(
            ("counterClaimIssued:" + SERVICE_REQUEST_REFERENCE).getBytes(StandardCharsets.UTF_8));
        ArgumentCaptor<SystemEventAction> actionCaptor = ArgumentCaptor.forClass(SystemEventAction.class);
        verify(systemEventExecutor).execute(eq(CASE_REFERENCE), eq(expectedKey), actionCaptor.capture());

        // running the action applies the fee update, transitions the counterclaim and describes the event
        SystemEventResult result = actionCaptor.getValue().execute(null);
        assertThat(feePaymentEntity.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(feePaymentEntity.getExternalReference()).isEqualTo(PAYMENT_REFERENCE);
        verify(feePaymentRepository).save(feePaymentEntity);
        assertThat(counterClaimEntity.getStatus()).isEqualTo(COUNTER_CLAIM_ISSUED);
        assertThat(counterClaimEntity.getClaimIssuedDate()).isEqualTo(LocalDateTime.of(2026, 6, 1, 10, 0));
        verify(counterClaimRepository).save(counterClaimEntity);
        assertThat(result.eventId()).isEqualTo("counterClaimIssued");
        assertThat(result.eventName()).isEqualTo("Counterclaim issued");
        assertThat(result.state()).isEmpty();

        // side effects run once, after commit
        verify(schedulerClient).scheduleIfNotExists(taskInstanceCaptor.capture());
        TaskInstance<?> taskInstance = taskInstanceCaptor.getValue().getTaskInstance();
        assertThat(taskInstance.getTaskName()).isEqualTo(COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR.getTaskName());
        CounterClaimTaskData data = (CounterClaimTaskData) taskInstance.getData();
        assertThat(data.getCounterClaimId()).isEqualTo(counterClaimId);
        verify(counterClaimFormScheduler).scheduleCounterClaimFormGeneration(counterClaimId);
    }

    @Test
    void replayedCallback_SkipsSideEffects() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        CounterClaimEntity counterClaimEntity = createCounterClaimEntity(counterClaimId,
                                                                         PENDING_COUNTER_CLAIM_ISSUED, partyId);
        stubTaskData(partyId, counterClaimId);
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(systemEventExecutor.execute(eq(CASE_REFERENCE), any(UUID.class), any()))
            .thenReturn(new SystemEventExecutionResult(1L, REPLAYED));

        underTest.handle(paidCallback(), feePayment());

        verifyNoInteractions(schedulerClient, counterClaimFormScheduler, translationWAService);
    }

    @Test
    void shouldNotIssueCounterClaimWhenAlreadyIssued() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        LocalDateTime existingIssuedDate = LocalDateTime.of(2026, 5, 1, 9, 0);
        CounterClaimEntity counterClaimEntity = createCounterClaimEntity(counterClaimId,
                                                                         COUNTER_CLAIM_ISSUED, partyId);
        counterClaimEntity.setClaimIssuedDate(existingIssuedDate);
        stubTaskData(partyId, counterClaimId);
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));

        underTest.handle(paidCallback(), feePayment());

        assertThat(counterClaimEntity.getStatus()).isEqualTo(COUNTER_CLAIM_ISSUED);
        assertThat(counterClaimEntity.getClaimIssuedDate()).isEqualTo(existingIssuedDate);
        verifyNoInteractions(systemEventExecutor, feePaymentRepository, schedulerClient,
                             counterClaimFormScheduler, translationWAService);
    }

    @Test
    void shouldNotUpdateCounterClaimWhenPaymentIsNotPaid() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        CounterClaimEntity counterClaimEntity = createCounterClaimEntity(counterClaimId,
                                                                         PENDING_COUNTER_CLAIM_ISSUED, partyId);
        stubTaskData(partyId, counterClaimId);
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));

        underTest.handle(callbackWithStatus("Not paid"), feePayment());

        assertThat(counterClaimEntity.getStatus()).isEqualTo(PENDING_COUNTER_CLAIM_ISSUED);
        verifyNoInteractions(systemEventExecutor, feePaymentRepository, schedulerClient,
                             counterClaimFormScheduler, translationWAService);
    }

    @Test
    void shouldThrowWhenCounterClaimNotFound() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        stubTaskData(partyId, counterClaimId);
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.handle(paidCallback(), feePayment()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Counterclaim not found");
        verifyNoInteractions(systemEventExecutor);
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    void shouldCreateTranslateTaskWhenCounterClaimPaymentConfirmed(LanguageUsed languageUsed) throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder().id(partyId).build();

        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();

        DefendantResponseEntity defendantResponse = DefendantResponseEntity.builder()
            .id(1)
            .party(party)
            .languageUsed(languageUsed)
            .build();
        pcsCaseEntity.setDefendantResponses(List.of(defendantResponse));

        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(PENDING_COUNTER_CLAIM_ISSUED)
            .party(party)
            .pcsCase(pcsCaseEntity)
            .build();

        DocumentEntity activeDocument = DocumentEntity.builder()
            .fileName("counterclaim-evidence.pdf")
            .counterClaim(counterClaimEntity)
            .build();
        DocumentEntity removedDocument = DocumentEntity.builder()
            .counterClaim(counterClaimEntity)
            .removed(true)
            .build();
        DocumentEntity noCounterClaimDocument = DocumentEntity.builder().build();
        DocumentEntity otherCounterClaimDocument = DocumentEntity.builder()
            .counterClaim(CounterClaimEntity.builder().id(UUID.randomUUID()).build())
            .build();
        pcsCaseEntity.setDocuments(
            List.of(activeDocument, removedDocument, noCounterClaimDocument, otherCounterClaimDocument));

        stubTaskData(partyId, counterClaimId);
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(translationWAService.isTranslationRequired(languageUsed)).thenReturn(true);
        when(systemEventExecutor.execute(eq(CASE_REFERENCE), any(UUID.class), any()))
            .thenReturn(new SystemEventExecutionResult(1L, EXECUTED));

        underTest.handle(paidCallback(), feePayment());

        verify(translationWAService).createTranslateDefendantSubmittedDocumentTask(
            pcsCaseEntity, party, List.of(activeDocument));
        verify(counterClaimFormScheduler).scheduleCounterClaimFormGeneration(counterClaimId);
        verify(schedulerClient).scheduleIfNotExists(any());
    }

    @Test
    void shouldNotCreateTranslateTaskWhenResponseLanguageIsEnglish() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder().id(partyId).build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        DefendantResponseEntity defendantResponse = DefendantResponseEntity.builder()
            .id(1)
            .party(party)
            .languageUsed(LanguageUsed.ENGLISH)
            .build();
        pcsCaseEntity.setDefendantResponses(List.of(defendantResponse));

        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(PENDING_COUNTER_CLAIM_ISSUED)
            .party(party)
            .pcsCase(pcsCaseEntity)
            .build();

        stubTaskData(partyId, counterClaimId);
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(systemEventExecutor.execute(eq(CASE_REFERENCE), any(UUID.class), any()))
            .thenReturn(new SystemEventExecutionResult(1L, EXECUTED));

        underTest.handle(paidCallback(), feePayment());

        verify(translationWAService).createTranslateDefendantSubmittedDocumentTask(pcsCaseEntity, party, List.of());
    }

    @Test
    void shouldNotCreateTranslateTaskWhenNoCounterClaimDocumentsUploaded() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder().id(partyId).build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        DefendantResponseEntity defendantResponse = DefendantResponseEntity.builder()
            .id(1)
            .party(party)
            .languageUsed(LanguageUsed.WELSH)
            .build();
        pcsCaseEntity.setDefendantResponses(List.of(defendantResponse));

        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(PENDING_COUNTER_CLAIM_ISSUED)
            .party(party)
            .pcsCase(pcsCaseEntity)
            .build();

        stubTaskData(partyId, counterClaimId);
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(translationWAService.isTranslationRequired(LanguageUsed.WELSH)).thenReturn(true);
        when(systemEventExecutor.execute(eq(CASE_REFERENCE), any(UUID.class), any()))
            .thenReturn(new SystemEventExecutionResult(1L, EXECUTED));

        underTest.handle(paidCallback(), feePayment());

        verify(translationWAService).createTranslateDefendantSubmittedDocumentTask(pcsCaseEntity, party, List.of());
    }

    @Test
    void shouldThrowWhenTaskDataCannotBeParsed() throws Exception {
        FeePaymentEntity feePaymentEntity = feePayment();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class)))
            .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "invalid"));

        assertThatThrownBy(() -> underTest.handle(paidCallback(), feePaymentEntity))
            .isInstanceOf(PaymentCallbackException.class)
            .hasMessageContaining("Unable to process");
        verifyNoInteractions(systemEventExecutor);
    }

    @Test
    void shouldThrowWhenTaskDataDoesNotContainCounterClaimId() throws Exception {
        UUID partyId = UUID.randomUUID();
        stubTaskData(partyId, null);

        assertThatThrownBy(() -> underTest.handle(paidCallback(), feePayment()))
            .isInstanceOf(PaymentCallbackException.class)
            .hasMessageContaining("missing relatedEntityId");
        verifyNoInteractions(systemEventExecutor);
    }

    @Test
    void handlesItsOwnTransactionOnlyWhenPaid() {
        assertThat(underTest.handlesOwnTransaction(paidCallback())).isTrue();
        assertThat(underTest.handlesOwnTransaction(callbackWithStatus("Partially paid"))).isFalse();
        assertThat(underTest.handlesOwnTransaction(callbackWithStatus("Not paid"))).isFalse();
    }

    private void stubTaskData(UUID partyId, UUID counterClaimId) throws Exception {
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class)))
            .thenReturn(createFeesAndPayTaskData(partyId, counterClaimId));
    }

    private FeePaymentEntity feePayment() {
        return FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.NOT_PAID)
            .taskData("task-data")
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

    private static CounterClaimEntity createCounterClaimEntity(UUID counterClaimId,
                                                               CounterClaimState counterClaimState,
                                                               UUID partyId) {
        return CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(counterClaimState)
            .party(PartyEntity.builder().id(partyId).build())
            .pcsCase(PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build())
            .build();
    }

    private static FeesAndPayTaskData createFeesAndPayTaskData(UUID partyId, UUID counterClaimId) {
        return FeesAndPayTaskData.builder()
            .feeDetails(FeeDetails.builder().feeAmount(BigDecimal.TEN).build())
            .caseReference(CASE_REFERENCE)
            .ccdCaseNumber(String.valueOf(CASE_REFERENCE))
            .responsiblePartyId(partyId)
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE)
            .relatedEntityId(counterClaimId)
            .build();
    }
}
