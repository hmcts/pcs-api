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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimTaskData;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormScheduler;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.COUNTER_CLAIM_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.task.CounterClaimIssuedNotificationTaskComponent.COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class CounterClaimPaymentCallbackHandlerTest {

    @Mock
    private CounterClaimRepository counterClaimRepository;
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

    @InjectMocks
    private CounterClaimPaymentCallbackHandler underTest;

    private static final Clock FIXED_UTC_CLOCK = Clock.fixed(
        Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        underTest = new CounterClaimPaymentCallbackHandler(counterClaimRepository,
                                                           schedulerClient, counterClaimFormScheduler,
                                                           translationWAService,
                                                           objectMapper, FIXED_UTC_CLOCK);
    }

    @Test
    void shouldSetCounterClaimIssuedWhenPaymentIsPaid() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        CounterClaimEntity counterClaimEntity = createCounterClaimEntity(counterClaimId,
                                                                         PENDING_COUNTER_CLAIM_ISSUED,
                                                                         partyId
        );

        FeesAndPayTaskData taskData = createFeesAndPayTaskData(partyId, counterClaimId);

        String paymentReference = "PAY-123";
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.PAID)
            .taskData("task-data")
            .externalReference(paymentReference)
            .build();

        PaymentStatusCallback callback = PaymentStatusCallback.builder().build();

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);

        underTest.handle(callback, feePaymentEntity);

        assertThat(counterClaimEntity.getStatus()).isEqualTo(CounterClaimState.COUNTER_CLAIM_ISSUED);
        assertThat(counterClaimEntity.getClaimIssuedDate()).isEqualTo(LocalDateTime.of(2026, 6, 1, 10, 0));

        verify(schedulerClient).scheduleIfNotExists(taskInstanceCaptor.capture());

        SchedulableInstance<?> schedulableInstance = taskInstanceCaptor.getValue();

        TaskInstance<?> taskInstance = schedulableInstance.getTaskInstance();
        assertThat(taskInstance.getTaskName()).isEqualTo(COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR.getTaskName());
        CounterClaimTaskData data = (CounterClaimTaskData) taskInstance.getData();
        assertThat(data.getCounterClaimId()).isEqualTo(counterClaimId);
        assertThat(data.getPaymentReference()).isEqualTo(paymentReference);

        verify(counterClaimFormScheduler).scheduleCounterClaimFormGeneration(counterClaimId);
    }

    @Test
    void shouldNotIssueCounterClaimWhenAlreadyIssued() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        LocalDateTime existingIssuedDate = LocalDateTime.of(2026, 5, 1, 9, 0);

        CounterClaimEntity counterClaimEntity = createCounterClaimEntity(counterClaimId, COUNTER_CLAIM_ISSUED, partyId);
        counterClaimEntity.setClaimIssuedDate(existingIssuedDate);

        FeesAndPayTaskData taskData = createFeesAndPayTaskData(partyId, counterClaimId);

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

        assertThat(counterClaimEntity.getStatus()).isEqualTo(CounterClaimState.COUNTER_CLAIM_ISSUED);
        assertThat(counterClaimEntity.getClaimIssuedDate()).isEqualTo(existingIssuedDate);
    }

    @Test
    void shouldNotUpdateCounterClaimWhenPaymentIsNotPaid() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        CounterClaimEntity counterClaimEntity = createCounterClaimEntity(counterClaimId,
                                                                         PENDING_COUNTER_CLAIM_ISSUED,
                                                                         partyId
        );

        FeesAndPayTaskData taskData = createFeesAndPayTaskData(partyId, counterClaimId);

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
    }

    @Test
    void shouldThrowWhenCounterClaimNotFound() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        FeesAndPayTaskData taskData = createFeesAndPayTaskData(partyId, counterClaimId);
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.PAID)
            .taskData("task-data")
            .build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder()
            .serviceRequestStatus(PaymentStatus.PAID.getValue())
            .payment(Payment.builder().paymentReference("RC-127").build())
            .build();

        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.handle(callback, feePaymentEntity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Counterclaim not found");
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    void shouldCreateTranslateTaskWhenCounterClaimPaymentConfirmed(LanguageUsed languageUsed) throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder().id(partyId).build();

        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(1234567890123456L)
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

        FeesAndPayTaskData taskData = createFeesAndPayTaskData(partyId, counterClaimId);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        when(translationWAService.isTranslationRequired(languageUsed)).thenReturn(true);

        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.PAID)
            .taskData("task-data")
            .externalReference("PAY-999")
            .build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder().build();

        underTest.handle(callback, feePaymentEntity);

        assertThat(counterClaimEntity.getStatus()).isEqualTo(CounterClaimState.COUNTER_CLAIM_ISSUED);
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

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(1234567890123456L)
            .build();

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

        FeesAndPayTaskData taskData = createFeesAndPayTaskData(partyId, counterClaimId);
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.PAID)
            .taskData("task-data")
            .build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder().build();

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);

        underTest.handle(callback, feePaymentEntity);

        verify(translationWAService).createTranslateDefendantSubmittedDocumentTask(pcsCaseEntity, party, List.of());
    }

    @Test
    void shouldNotCreateTranslateTaskWhenNoCounterClaimDocumentsUploaded() throws Exception {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder().id(partyId).build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(1234567890123456L)
            .build();

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

        FeesAndPayTaskData taskData = createFeesAndPayTaskData(partyId, counterClaimId);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaimEntity));
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        when(translationWAService.isTranslationRequired(LanguageUsed.WELSH)).thenReturn(true);

        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.PAID)
            .taskData("task-data")
            .build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder().build();

        underTest.handle(callback, feePaymentEntity);

        verify(translationWAService).createTranslateDefendantSubmittedDocumentTask(pcsCaseEntity, party, List.of());
    }

    @Test
    void shouldThrowWhenTaskDataCannotBeParsed() throws Exception {
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .paymentStatus(PaymentStatus.PAID)
            .taskData("invalid-json")
            .build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder()
            .serviceRequestStatus(PaymentStatus.PAID.getValue())
            .payment(Payment.builder().paymentReference("RC-128").build())
            .build();

        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class)))
            .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "invalid"));

        assertThatThrownBy(() -> underTest.handle(callback, feePaymentEntity))
            .isInstanceOf(PaymentCallbackException.class)
            .hasMessageContaining("Unable to process");
    }

    @Test
    void shouldThrowWhenTaskDataDoesNotContainCounterClaimId() throws Exception {
        UUID partyId = UUID.randomUUID();
        FeesAndPayTaskData taskData = createFeesAndPayTaskData(partyId, null);
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

    private static CounterClaimEntity createCounterClaimEntity(UUID counterClaimId,
                                                               CounterClaimState counterClaimState,
                                                               UUID partyId) {
        return CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(counterClaimState)
            .party(PartyEntity.builder().id(partyId).build())
            .build();
    }

    private static FeesAndPayTaskData createFeesAndPayTaskData(UUID partyId, UUID counterClaimId) {
        return FeesAndPayTaskData.builder()
            .feeDetails(FeeDetails.builder().feeAmount(BigDecimal.TEN).build())
            .caseReference(1234567890123456L)
            .ccdCaseNumber("1234567890123456")
            .responsiblePartyId(partyId)
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE)
            .relatedEntityId(counterClaimId)
            .build();
    }

}
